package com.duoc.plomoredsocial.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.duoc.plomoredsocial.R
import com.duoc.plomoredsocial.model.AuthUser
import com.duoc.plomoredsocial.ui.components.*

@Composable
internal fun HomeScreen(user: AuthUser?, onLogin: () -> Unit, onProfile: () -> Unit) {
    MygosScreen {
        Text(stringResource(R.string.nav_brand), color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.home_eyebrow), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.home_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        MygosCard {
            Icon(Icons.Filled.AccountCircle, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Text(if (user == null) stringResource(R.string.nav_account)
                else stringResource(R.string.home_greeting, user.name), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (user == null) R.string.home_login_description else R.string.home_session_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            MygosPrimaryButton(stringResource(if (user == null) R.string.nav_login else R.string.home_profile),
                if (user == null) onLogin else onProfile)
        }
    }
}
