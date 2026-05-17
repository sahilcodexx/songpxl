package com.theveloper.playpix.di

import com.theveloper.playpix.data.jiosaavn.JioSaavnApiService
import com.theveloper.playpix.data.soundcloud.SoundCloudApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JioSaavnRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SoundCloudRetrofit

@Module
@InstallIn(SingletonComponent::class)
object StreamingModule {

    @Provides
    @Singleton
    @JioSaavnRetrofit
    fun provideJioSaavnRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://saavn-api-xi.vercel.app/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideJioSaavnApiService(@JioSaavnRetrofit retrofit: Retrofit): JioSaavnApiService {
        return retrofit.create(JioSaavnApiService::class.java)
    }

    @Provides
    @Singleton
    @SoundCloudRetrofit
    fun provideSoundCloudRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api-v2.soundcloud.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSoundCloudApiService(@SoundCloudRetrofit retrofit: Retrofit): SoundCloudApiService {
        return retrofit.create(SoundCloudApiService::class.java)
    }
}
