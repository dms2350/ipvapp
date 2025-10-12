package com.dms2350.iptvapp.di

import android.content.Context
import com.dms2350.iptvapp.utils.TVBoxStabilityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {

    @Provides
    @Singleton
    fun provideTVBoxStabilityManager(
        @ApplicationContext context: Context
    ): TVBoxStabilityManager {
        return TVBoxStabilityManager(context)
    }
}