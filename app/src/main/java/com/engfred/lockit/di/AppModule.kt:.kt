package com.engfred.lockit.di

import android.content.Context
import androidx.room.Room
import com.engfred.lockit.data.local.AppDatabase
import com.engfred.lockit.data.repository.AppRepositoryImpl
import com.engfred.lockit.domain.repository.AppRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "app_locker_db").build()
    }

    @Provides
    @Singleton
    fun provideAppRepository(db: AppDatabase, @ApplicationContext context: Context): AppRepository {
        return AppRepositoryImpl(db, context)
    }
}