package com.example.vidplay.di

import com.example.vidplay.data.repository.AuthRepositoryImpl
import com.example.vidplay.data.source.remote.AuthApiService
import com.example.vidplay.data.source.remote.RetrofitClient
import com.example.vidplay.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthApiService(): AuthApiService {
        return RetrofitClient.authApiService
    }

    @Provides
    @Singleton
    fun provideAuthRepository(authApiService: AuthApiService): AuthRepository {
        return AuthRepositoryImpl(authApiService)
    }
}
