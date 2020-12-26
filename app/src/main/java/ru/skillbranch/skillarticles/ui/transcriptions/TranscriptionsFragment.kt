package ru.skillbranch.skillarticles.ui.transcriptions

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_transcriptions.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.base.BaseFragment
import ru.skillbranch.skillarticles.viewmodels.transcriptions.TranscriptionsViewModel

@AndroidEntryPoint
class TranscriptionsFragment : BaseFragment<TranscriptionsViewModel>() {

    override val viewModel: TranscriptionsViewModel by viewModels()
    override val layout = R.layout.fragment_transcriptions

    override fun setupViews() {
        btn_reset.setOnClickListener {
            viewModel.resetAllPreferences(root)
        }
    }
}
