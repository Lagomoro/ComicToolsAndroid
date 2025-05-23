package com.moroworks.comictools.ui.epaper

import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.moroworks.comictools.MainActivity
import com.moroworks.comictools.GoodDisplayNFCePaperActivity
import com.moroworks.comictools.databinding.FragmentEpaperBinding


class GoodDisplayNFCePaperFragment : Fragment() {

    private var _binding: FragmentEpaperBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(GoodDisplayNFCePaperViewModel::class.java)

        _binding = FragmentEpaperBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textViewNfcResult: TextView = binding.textNfcResult
        galleryViewModel.textWaitNFCLaunch.observe(viewLifecycleOwner) {
            textViewNfcResult.text = it
        }

        val buttonNfcStart: Button = binding.buttonNfcStart
        galleryViewModel.textBtnNfcStart.observe(viewLifecycleOwner) {
            buttonNfcStart.text = it
        }
        buttonNfcStart.setOnClickListener(NfcStartClickListener(this))

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class NfcStartClickListener(private var fragment: GoodDisplayNFCePaperFragment) : View.OnClickListener {

        override fun onClick(v: View) {
            fragment.requireActivity().startActivity(Intent(fragment.activity, GoodDisplayNFCePaperActivity::class.java))
        }

    }

}