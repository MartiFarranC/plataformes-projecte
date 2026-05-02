package com.example.testposturai.chat

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(val container: LinearLayout, val textView: TextView) :
        RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val container = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 8, 10, 8)
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }

        val textView = TextView(parent.context).apply {
            textSize = 15f
            setPadding(20, 14, 20, 14)
        }

        container.addView(textView)
        return ChatViewHolder(container, textView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        val params = holder.textView.layoutParams as LinearLayout.LayoutParams? ?: LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        if (message.isUser) {
            holder.container.gravity = Gravity.END
            holder.textView.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 28f
                setColor(Color.parseColor("#1F5EFF"))
            }
            holder.textView.setTextColor(Color.WHITE)
            params.gravity = Gravity.END
        } else {
            holder.container.gravity = Gravity.START
            holder.textView.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 28f
                setColor(Color.parseColor("#EEF2F8"))
            }
            holder.textView.setTextColor(Color.parseColor("#1C2434"))
            params.gravity = Gravity.START
        }

        holder.textView.layoutParams = params
        holder.textView.text = message.text
    }

    override fun getItemCount(): Int = messages.size
}
