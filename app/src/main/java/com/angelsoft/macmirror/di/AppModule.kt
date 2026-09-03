package com.angelsoft.macmirror.di

import com.angelsoft.macmirror.data.PreferencesManager
import com.angelsoft.macmirror.network.NsdHelper
import com.angelsoft.macmirror.security.CryptoManager
import com.angelsoft.macmirror.ui.MainViewModel
import com.angelsoft.macmirror.ui.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CryptoManager(androidContext()) }
    single { PreferencesManager(androidContext()) }
    single { NsdHelper(androidContext()) }
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(androidContext(), get()) }
}
