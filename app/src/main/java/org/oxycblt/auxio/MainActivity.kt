/*
 * Copyright (c) 2021 Auxio Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import org.oxycblt.auxio.databinding.ActivityMainBinding
import org.oxycblt.auxio.music.system.IndexerService
import org.oxycblt.auxio.playback.PlaybackViewModel
import org.oxycblt.auxio.playback.system.PlaybackService
import org.oxycblt.auxio.settings.Settings
import org.oxycblt.auxio.util.androidViewModels
import org.oxycblt.auxio.util.isNight
import org.oxycblt.auxio.util.logD
import org.oxycblt.auxio.util.systemBarInsetsCompat

/**
 * The single [AppCompatActivity] for Auxio.
 *
 * TODO: Add error screens.
 *
 * TODO: Custom language support
 *
 * TODO: Add multi-select
 *
 * @author OxygenCobalt
 */
class MainActivity : AppCompatActivity() {
    private val playbackModel: PlaybackViewModel by androidViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupTheme()

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)

        logD("Activity created")
    }

    override fun onStart() {
        super.onStart()

        startService(Intent(this, IndexerService::class.java))
        startService(Intent(this, PlaybackService::class.java))

        if (!startIntentDelayedAction(intent)) {
            playbackModel.startDelayedAction(PlaybackViewModel.DelayedAction.RestoreState)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        startIntentDelayedAction(intent)
    }

    private fun startIntentDelayedAction(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }

        if (intent.getBooleanExtra(KEY_INTENT_USED, false)) {
            // Don't commit the action, but also return that the intent was applied.
            // This is because onStart can run multiple times, and thus we really don't
            // want to return false and override the original delayed action with a
            // RestoreState action.
            return true
        }

        intent.putExtra(KEY_INTENT_USED, true)

        val action =
            when (intent.action) {
                Intent.ACTION_VIEW ->
                    PlaybackViewModel.DelayedAction.Open(intent.data ?: return false)
                AuxioApp.INTENT_KEY_SHORTCUT_SHUFFLE -> {
                    PlaybackViewModel.DelayedAction.ShuffleAll
                }
                else -> return false
            }

        playbackModel.startDelayedAction(action)

        return true
    }

    private fun setupTheme() {
        val settings = Settings(this)

        // Disable theme customization above Android 12, as it's far enough in as a version to
        // the point where most phones should have an automatic option for light/dark theming.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            AppCompatDelegate.setDefaultNightMode(settings.theme)
        }

        // The black theme has a completely separate set of styles since style attributes cannot
        // be modified at runtime.
        if (isNight && settings.useBlackTheme) {
            logD("Applying black theme [accent ${settings.accent}]")
            setTheme(settings.accent.blackTheme)
        } else {
            logD("Applying normal theme [accent ${settings.accent}]")
            setTheme(settings.accent.theme)
        }
    }

    private fun setupEdgeToEdge(contentView: View) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        contentView.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.systemBarInsetsCompat
            view.updatePadding(left = bars.left, right = bars.right)
            insets
        }
    }

    companion object {
        private const val KEY_INTENT_USED = BuildConfig.APPLICATION_ID + ".key.FILE_INTENT_USED"
    }
}
