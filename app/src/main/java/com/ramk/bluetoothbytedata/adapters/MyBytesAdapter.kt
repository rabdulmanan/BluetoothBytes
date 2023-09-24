package com.ramk.bluetoothbytedata.adapters

import android.app.ActionBar.LayoutParams
import android.text.Editable
import android.text.TextWatcher
import android.util.LayoutDirection
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.ramk.bluetoothbytedata.databinding.ByteItemLayoutBinding
import com.ramk.bluetoothbytedata.models.MyByte
import java.lang.Exception

class MyBytesAdapter(private var myBytesList: List<MyByte>) :
    RecyclerView.Adapter<MyBytesAdapter.BytesViewHolder>() {

    override fun getItemCount(): Int {
        return myBytesList.size
    }

    fun setData(list : List<MyByte>){
        this.myBytesList = list
        notifyDataSetChanged()
    }

    fun getData():List<MyByte>{
        return myBytesList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BytesViewHolder {
        val mBinding = ByteItemLayoutBinding.inflate(LayoutInflater.from(parent.context))

        return BytesViewHolder(mBinding)
    }

    override fun onBindViewHolder(holder: BytesViewHolder, position: Int) {
        val myByte = myBytesList[position]
        holder.displayMyByte(myByte, position)
    }

    inner class BytesViewHolder(val mBinding: ByteItemLayoutBinding) :
        RecyclerView.ViewHolder(mBinding.root), BitChangeListener {


        private var adapterIndex = 0
        init {
            mBinding.bit1.addTextChangedListener(EditTextChangeListener(mBinding.bit1, this))
            mBinding.bit2.addTextChangedListener(EditTextChangeListener(mBinding.bit2, this))
            mBinding.bit3.addTextChangedListener(EditTextChangeListener(mBinding.bit3, this))
            mBinding.bit4.addTextChangedListener(EditTextChangeListener(mBinding.bit4, this))
            mBinding.bit5.addTextChangedListener(EditTextChangeListener(mBinding.bit5, this))
            mBinding.bit6.addTextChangedListener(EditTextChangeListener(mBinding.bit6, this))
            mBinding.bit7.addTextChangedListener(EditTextChangeListener(mBinding.bit7, this))
            mBinding.bit8.addTextChangedListener(EditTextChangeListener(mBinding.bit8, this))
        }

        override fun onBitChange() {
            val bit1Value = mBinding.bit1.text.toString().toInt()
            val bit2Value = mBinding.bit2.text.toString().toInt()
            val bit3Value = mBinding.bit3.text.toString().toInt()
            val bit4Value = mBinding.bit4.text.toString().toInt()
            val bit5Value = mBinding.bit5.text.toString().toInt()
            val bit6Value = mBinding.bit6.text.toString().toInt()
            val bit7Value = mBinding.bit7.text.toString().toInt()
            val bit8Value = mBinding.bit8.text.toString().toInt()

            myBytesList[adapterIndex].bit1 = bit1Value == 1
            myBytesList[adapterIndex].bit2 = bit2Value == 1
            myBytesList[adapterIndex].bit3 = bit3Value == 1
            myBytesList[adapterIndex].bit4 = bit4Value == 1
            myBytesList[adapterIndex].bit5 = bit5Value == 1
            myBytesList[adapterIndex].bit6 = bit6Value == 1
            myBytesList[adapterIndex].bit7 = bit7Value == 1
            myBytesList[adapterIndex].bit8 = bit8Value == 1


            mBinding.totalBytes.text = myBytesList[adapterIndex].getDisplayBytes()
        }

        fun displayMyByte(myByte: MyByte, position: Int) {

            val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

            layoutParams.weight = 13.6f

            mBinding.root.layoutParams = layoutParams

            adapterIndex = position
            mBinding.timeIndex.text = "${position + 1}"
            mBinding.totalBytes.text = myByte.getDisplayBytes()


            mBinding.bit1.setText((if (myByte.bit1) 1 else 0).toString())
            mBinding.bit2.setText((if (myByte.bit2) 1 else 0).toString())
            mBinding.bit3.setText((if (myByte.bit3) 1 else 0).toString())
            mBinding.bit4.setText((if (myByte.bit4) 1 else 0).toString())
            mBinding.bit5.setText((if (myByte.bit5) 1 else 0).toString())
            mBinding.bit6.setText((if (myByte.bit6) 1 else 0).toString())
            mBinding.bit7.setText((if (myByte.bit7) 1 else 0).toString())
            mBinding.bit8.setText((if (myByte.bit8) 1 else 0).toString())


        }

    }

    interface BitChangeListener {
        fun onBitChange()
    }
    class EditTextChangeListener(
        private val editText: EditText,
        private val changeListener: BitChangeListener
    ) : TextWatcher {
        override fun afterTextChanged(s: Editable) {

            try {
                val input = s.toString().toInt()
                if (input > 1) {
                    editText.setText("1")
                } else if (input < 0) {
                    editText.setText("0")
                }
                changeListener.onBitChange()
            }catch (ex: Exception){
                ex.printStackTrace()
            }
        }

        override fun beforeTextChanged(
            s: CharSequence, start: Int,
            count: Int, after: Int
        ) {
        }

        override fun onTextChanged(
            s: CharSequence, start: Int,
            before: Int, count: Int
        ) {
        }


    }
}