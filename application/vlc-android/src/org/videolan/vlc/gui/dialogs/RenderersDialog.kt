/*****************************************************************************
 * RenderersDialog.java
 *
 * Copyright © 2017 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package org.videolan.vlc.gui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.libvlc.RendererItem
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.RendererDelegate
import org.videolan.vlc.databinding.DialogRenderersBinding
import org.videolan.vlc.databinding.ItemRendererBinding
import org.videolan.vlc.gui.DiffUtilAdapter
import org.videolan.vlc.gui.helpers.SelectorViewHolder
import org.videolan.vlc.gui.helpers.UiTools

const private val TAG = "VLC/RenderersDialog"

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class RenderersDialog : DialogFragment() {
    private var renderers = RendererDelegate.renderers.value
    private lateinit var mBinding: DialogRenderersBinding
    private val mAdapter = RendererAdapter()
    private val mClickHandler = RendererClickhandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RendererDelegate.renderers.observe(this) {
            if (it !== null) {
                renderers = it
                mAdapter.update(it)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        mBinding = DialogRenderersBinding.inflate(inflater, null)
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(mBinding.root)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setStyle(DialogFragment.STYLE_NO_FRAME, 0)
        mBinding = DialogRenderersBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mBinding.holder = mClickHandler;
        mBinding.renderersList.layoutManager = LinearLayoutManager(view.context)
        mBinding.renderersList.adapter = mAdapter
        mBinding.renderersDisconnect.isEnabled = PlaybackService.hasRenderer()
        mBinding.renderersDisconnect.setTextColor(ContextCompat.getColor(view.context, if (PlaybackService.hasRenderer()) R.color.orange800 else R.color.grey400))
        mAdapter.update(renderers)
    }

    private inner class RendererAdapter : DiffUtilAdapter<RendererItem, SelectorViewHolder<ItemRendererBinding>>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectorViewHolder<ItemRendererBinding> {
            val binding = ItemRendererBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            binding.clicHandler = mClickHandler
            return SelectorViewHolder(binding)
        }

        override fun onBindViewHolder(holder: SelectorViewHolder<ItemRendererBinding>, position: Int) {
            holder.binding.renderer = renderers[position]
            if (renderers[position] == PlaybackService.renderer.value)
            holder.binding.rendererName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.orange800))
        }

        override fun getItemCount() = dataset.size

        override fun onUpdateFinished() {}
    }

    inner class RendererClickhandler {
        fun connect(item: RendererItem?) {
            PlaybackService.renderer.value = item
            dismissAllowingStateLoss()
            item?.run {
                activity?.window?.findViewById<View>(R.id.audio_player_container)?.let {
                    UiTools.snacker(requireActivity(), getString(R.string.casting_connected_renderer, displayName))
                }
            }
        }
    }
}
