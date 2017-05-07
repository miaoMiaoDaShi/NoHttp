/*
 * Copyright © Yan Zhenjie. All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yanzhenjie.nohttp.cache;

import android.content.Context;
import android.text.TextUtils;

import com.yanzhenjie.nohttp.Logger;
import com.yanzhenjie.nohttp.tools.CacheStore;
import com.yanzhenjie.nohttp.tools.Encryption;
import com.yanzhenjie.nohttp.tools.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>You must remember to check the runtime permissions.</p>
 * Created by Yan Zhenjie on Jan 10, 2016.
 */
public class DiskCacheStore implements CacheStore<CacheEntity> {

    public static Builder newBuilder(Context context) {
        return new Builder(context);
    }

    private Lock mLock;
    private Encryption mEncryption;
    private File mCacheDirectory;

    /**
     * @deprecated use {@link #newBuilder(Context)} instead.
     */
    @Deprecated
    public DiskCacheStore(Context context) {
        this(newBuilder(context));
    }

    /**
     * @deprecated use {@link #newBuilder(Context)} instead.
     */
    @Deprecated
    public DiskCacheStore(String cacheDirectory) {
        this(newBuilder(null).cacheDirectory(new File(cacheDirectory)));
    }

    private DiskCacheStore(Builder builder) {
        this.mLock = new ReentrantLock();
        this.mEncryption = new Encryption(DiskCacheStore.class.getSimpleName());
        this.mCacheDirectory = builder.mCacheDirectory;
    }

    @Override
    public CacheEntity get(String key) {
        mLock.lock();
        BufferedReader bufferedReader = null;
        try {
            if (TextUtils.isEmpty(key))
                return null;
            File file = new File(mCacheDirectory, getFileName(key));
            if (!file.exists() || file.isDirectory())
                return null;
            CacheEntity cacheEntity = new CacheEntity();

            bufferedReader = new BufferedReader(new FileReader(file));
            cacheEntity.setResponseHeadersJson(decrypt(bufferedReader.readLine()));
            cacheEntity.setDataBase64(decrypt(bufferedReader.readLine()));
            cacheEntity.setLocalExpireString(decrypt(bufferedReader.readLine()));
            return cacheEntity;
        } catch (Exception e) {
            IOUtils.delFileOrFolder(new File(mCacheDirectory, getFileName(key)));
            Logger.e(e);
        } finally {
            IOUtils.closeQuietly(bufferedReader);
            mLock.unlock();
        }
        return null;
    }

    @Override
    public CacheEntity replace(String key, CacheEntity cacheEntity) {
        mLock.lock();
        BufferedWriter bufferedWriter = null;
        try {
            if (TextUtils.isEmpty(key) || cacheEntity == null)
                return cacheEntity;
            initialize();
            File file = new File(mCacheDirectory, getFileName(key));

            IOUtils.createNewFile(file);

            bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(encrypt(cacheEntity.getResponseHeadersJson()));
            bufferedWriter.newLine();
            bufferedWriter.write(encrypt(cacheEntity.getDataBase64()));
            bufferedWriter.newLine();
            bufferedWriter.write(encrypt(cacheEntity.getLocalExpireString()));
            bufferedWriter.flush();
            bufferedWriter.close();
            return cacheEntity;
        } catch (Exception e) {
            IOUtils.delFileOrFolder(new File(mCacheDirectory, getFileName(key)));
            Logger.e(e);
            return null;
        } finally {
            IOUtils.closeQuietly(bufferedWriter);
            mLock.unlock();
        }
    }

    @Override
    public boolean remove(String key) {
        mLock.lock();
        try {
            return IOUtils.delFileOrFolder(new File(mCacheDirectory, getFileName(key)));
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public boolean clear() {
        mLock.lock();
        try {
            return IOUtils.delFileOrFolder(mCacheDirectory);
        } finally {
            mLock.unlock();
        }
    }

    private boolean initialize() {
        return IOUtils.createFolder(mCacheDirectory);
    }

    private String getFileName(String cacheKey) {
        return Encryption.getMD5ForString(cacheKey) + ".nohttp";
    }

    private String encrypt(String encryptionText) throws Exception {
        return mEncryption.encrypt(encryptionText);
    }

    private String decrypt(String cipherText) throws Exception {
        return mEncryption.decrypt(cipherText);
    }

    public static final class Builder {

        private Context mContext;
        private File mCacheDirectory;

        private Builder(Context context) {
            this.mContext = context.getApplicationContext();
            this.mCacheDirectory = mContext.getCacheDir();
        }

        public Builder cacheDirectory(File directory) {
            this.mCacheDirectory = directory;
            return this;
        }

        public DiskCacheStore build() {
            return new DiskCacheStore(this);
        }
    }

}
