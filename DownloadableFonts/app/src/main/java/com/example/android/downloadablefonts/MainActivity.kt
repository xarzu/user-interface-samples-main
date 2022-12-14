/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.downloadablefonts

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArraySet
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.example.android.downloadablefonts.Constants.ITALIC_DEFAULT
import com.example.android.downloadablefonts.Constants.WEIGHT_DEFAULT
import com.example.android.downloadablefonts.Constants.WEIGHT_MAX
import com.example.android.downloadablefonts.Constants.WIDTH_DEFAULT
import com.example.android.downloadablefonts.Constants.WIDTH_MAX
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var handler: Handler

    private lateinit var downloadableFontTextView: TextView
    private lateinit var widthSeekBar: SeekBar
    private lateinit var weightSeekBar: SeekBar
    private lateinit var italicSeekBar: SeekBar
    private lateinit var bestEffort: CheckBox
    private lateinit var requestDownloadButton: Button

    private lateinit var familyNameSet: ArraySet<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val handlerThread = HandlerThread("fonts")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        initializeSeekBars()
        familyNameSet = ArraySet<String>()
        familyNameSet.addAll(listOf(*resources.getStringArray(R.array.family_names)))

        downloadableFontTextView = findViewById(R.id.textview)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.family_names)
        )
        val familyNameInput = findViewById<TextInputLayout>(R.id.auto_complete_family_name_input)
        val autoCompleteFamilyName =
            findViewById<AutoCompleteTextView>(R.id.auto_complete_family_name)
        autoCompleteFamilyName.setAdapter<ArrayAdapter<String>>(adapter)
        autoCompleteFamilyName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence, start: Int, count: Int,
                after: Int
            ) {
                // No op
            }

            override fun onTextChanged(
                charSequence: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
                if (isValidFamilyName(charSequence.toString())) {
                    familyNameInput.isErrorEnabled = false
                    familyNameInput.error = ""
                } else {
                    familyNameInput.isErrorEnabled = true
                    familyNameInput.error = getString(R.string.invalid_family_name)
                }
            }

            override fun afterTextChanged(editable: Editable) {
                // No op
            }
        })

        requestDownloadButton = findViewById(R.id.button_request)
        requestDownloadButton.setOnClickListener(View.OnClickListener {
            val familyName = autoCompleteFamilyName.text.toString()
            if (!isValidFamilyName(familyName)) {
                familyNameInput.isErrorEnabled = true
                familyNameInput.error = getString(R.string.invalid_family_name)
                Toast.makeText(
                    this@MainActivity,
                    R.string.invalid_input,
                    Toast.LENGTH_SHORT
                ).show()
                return@OnClickListener
            }
            requestDownload(familyName)
            requestDownloadButton.isEnabled = false
        })
        bestEffort = findViewById(R.id.checkbox_best_effort)
    }

    private fun requestDownload(familyName: String) {
        val queryBuilder = QueryBuilder(
            familyName,
            width = progressToWidth(widthSeekBar.progress),
            weight = progressToWeight(weightSeekBar.progress),
            italic = progressToItalic(italicSeekBar.progress),
            bestEffort = bestEffort.isChecked
        )
        val query = queryBuilder.build()

        Log.d(TAG, "Requesting a font. Query: $query")
        val request = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            query,
            R.array.com_google_android_gms_fonts_certs
        )

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val callback = object : FontsContractCompat.FontRequestCallback() {
            override fun onTypefaceRetrieved(typeface: Typeface) {
                downloadableFontTextView.typeface = typeface
                progressBar.visibility = View.GONE
                requestDownloadButton.isEnabled = true
            }

            override fun onTypefaceRequestFailed(reason: Int) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.request_failed, reason), Toast.LENGTH_LONG
                )
                    .show()
                progressBar.visibility = View.GONE
                requestDownloadButton.isEnabled = true
            }
        }
        FontsContractCompat
            .requestFont(this@MainActivity, request, callback, handler)
    }

    private fun initializeSeekBars() {
        widthSeekBar = findViewById(R.id.seek_bar_width)
        val widthValue = (100 * WIDTH_DEFAULT.toFloat() / WIDTH_MAX.toFloat()).toInt()
        widthSeekBar.progress = widthValue
        val widthTextView = findViewById<TextView>(R.id.textview_width)
        widthTextView.text = widthValue.toString()
        widthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                widthTextView.text = progressToWidth(progress).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        weightSeekBar = findViewById(R.id.seek_bar_weight)
        val weightValue = WEIGHT_DEFAULT.toFloat() / WEIGHT_MAX.toFloat() * 100
        weightSeekBar.progress = weightValue.toInt()
        val weightTextView = findViewById<TextView>(R.id.textview_weight)
        weightTextView.text = WEIGHT_DEFAULT.toString()
        weightSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                weightTextView.text = progressToWeight(progress).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        italicSeekBar = findViewById<SeekBar>(R.id.seek_bar_italic)
        italicSeekBar.progress = ITALIC_DEFAULT.toInt()
        val italicTextView = findViewById<TextView>(R.id.textview_italic)
        italicTextView.text = ITALIC_DEFAULT.toString()
        italicSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                italicTextView.text = progressToItalic(progress).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun isValidFamilyName(familyName: String?) =
        familyName != null && familyNameSet.contains(familyName)

    /**
     * Converts progress from a SeekBar to the value of width.
     * @param progress is passed from 0 to 100 inclusive
     * *
     * @return the converted width
     */
    private fun progressToWidth(progress: Int): Float {
        return (if (progress == 0) 1 else progress * WIDTH_MAX / 100).toFloat()
    }

    /**
     * Converts progress from a SeekBar to the value of weight.
     * @param progress is passed from 0 to 100 inclusive
     * *
     * @return the converted weight
     */
    private fun progressToWeight(progress: Int) = when (progress) {
        0 -> {
            1 // The range of the weight is between (0, 1000) (exclusive)
        }
        100 -> {
            WEIGHT_MAX - 1 // The range of the weight is between (0, 1000) (exclusive)
        }
        else -> {
            WEIGHT_MAX * progress / 100
        }
    }

    /**
     * Converts progress from a SeekBar to the value of italic.
     * @param progress is passed from 0 to 100 inclusive.
     * *
     * @return the converted italic
     */
    private fun progressToItalic(progress: Int): Float {
        return progress.toFloat() / 100f
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
