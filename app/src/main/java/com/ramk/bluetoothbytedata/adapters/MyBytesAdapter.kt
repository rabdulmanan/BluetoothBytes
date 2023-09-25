package com.ramk.bluetoothbytedata.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.ramk.bluetoothbytedata.R
import com.ramk.bluetoothbytedata.databinding.ByteItemLayoutBinding
import com.ramk.bluetoothbytedata.models.MyByte
import com.ramk.bluetoothbytedata.utils.Utils
import java.lang.Exception

class MyBytesAdapter(private var myBytesList: List<MyByte>) :
    RecyclerView.Adapter<MyBytesAdapter.BytesViewHolder>() {

    init {

        if(myBytesList.isNotEmpty()){
            myBytesList[0].hasFocus = true
            myBytesList[0].focusedBit = 1
        }

    }

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

    private fun requestFocus(index:Int){

    }
    override fun onBindViewHolder(holder: BytesViewHolder, position: Int) {
        val myByte = myBytesList[position]
        holder.displayMyByte(myByte, position)
    }

    inner class BytesViewHolder(val mBinding: ByteItemLayoutBinding) :
        RecyclerView.ViewHolder(mBinding.root), BitChangeListener {

        private var isDataSetBySystem = false
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

        override fun onBitChange(editText: EditText) {
            if(!isDataSetBySystem) {
                val bit1Value = mBinding.bit1.text.toString().toInt()
                val bit2Value = mBinding.bit2.text.toString().toInt()
                val bit3Value = mBinding.bit3.text.toString().toInt()
                val bit4Value = mBinding.bit4.text.toString().toInt()
                val bit5Value = mBinding.bit5.text.toString().toInt()
                val bit6Value = mBinding.bit6.text.toString().toInt()
                val bit7Value = mBinding.bit7.text.toString().toInt()
                val bit8Value = mBinding.bit8.text.toString().toInt()

                val newList = ArrayList(myBytesList)
                newList[adapterIndex].bit1 = bit1Value == 1
                newList[adapterIndex].bit2 = bit2Value == 1
                newList[adapterIndex].bit3 = bit3Value == 1
                newList[adapterIndex].bit4 = bit4Value == 1
                newList[adapterIndex].bit5 = bit5Value == 1
                newList[adapterIndex].bit6 = bit6Value == 1
                newList[adapterIndex].bit7 = bit7Value == 1
                newList[adapterIndex].bit8 = bit8Value == 1


                for (i in 1..Utils.NUMBER_OF_BYTES) {

                    if(i-1 == adapterIndex){
                        newList[adapterIndex].hasFocus = true
                        val focusedBit = getNextFocusBit(editText)
                        newList[i - 1].focusedBit = focusedBit

                    }else {
                        newList[i - 1].hasFocus = false
                        newList[i - 1].focusedBit = 0
                    }
                }

                setData(newList)

            }
        }

        fun getNextFocusBit(editText: EditText):Int{
            when(editText.id){
                R.id.bit1 ->{
                    return 2
                }
                R.id.bit2 ->{
                    return 3
                }
                R.id.bit3 ->{
                    return 4
                }
                R.id.bit4 ->{
                    return 5
                }
                R.id.bit5 ->{
                    return 6
                }
                R.id.bit6 ->{
                    return 7
                }
                R.id.bit7 ->{
                    return 8
                }
            }

            return 1
        }

        fun displayMyByte(myByte: MyByte, position: Int) {

            isDataSetBySystem = true
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

            isDataSetBySystem = false

            mBinding.bit1.clearFocus()
            mBinding.bit2.clearFocus()
            mBinding.bit3.clearFocus()
            mBinding.bit4.clearFocus()
            mBinding.bit5.clearFocus()
            mBinding.bit6.clearFocus()
            mBinding.bit7.clearFocus()
            mBinding.bit8.clearFocus()


            if(myByte.hasFocus){
                when(myByte.focusedBit){

                    1 ->{
                        mBinding.bit1.requestFocus()
                    }
                    2 ->{
                        mBinding.bit2.requestFocus()
                    }
                    3 ->{
                        mBinding.bit3.requestFocus()
                    }
                    4 ->{
                        mBinding.bit4.requestFocus()
                    }
                    5 ->{
                        mBinding.bit5.requestFocus()
                    }
                    6 ->{
                        mBinding.bit6.requestFocus()
                    }
                    7 ->{
                        mBinding.bit7.requestFocus()
                    }
                    8 ->{
                        mBinding.bit8.requestFocus()
                    }

                }

            }

        }

    }

    interface BitChangeListener {
        fun onBitChange(editText: EditText)
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
                changeListener.onBitChange(editText)
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