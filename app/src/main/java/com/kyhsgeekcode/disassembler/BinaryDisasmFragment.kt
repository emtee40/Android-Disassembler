package com.kyhsgeekcode.disassembler

import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kyhsgeekcode.TAG
import com.kyhsgeekcode.disassembler.databinding.FragmentBinaryDisasmBinding
import com.kyhsgeekcode.disassembler.disasmtheme.ColorHelper
import com.kyhsgeekcode.disassembler.models.Architecture.CS_ARCH_ALL
import com.kyhsgeekcode.disassembler.models.Architecture.CS_ARCH_MAX
import com.kyhsgeekcode.disassembler.models.Architecture.getArchitecture
import timber.log.Timber
import java.util.*

class BinaryDisasmFragment : Fragment() {
    private var _binding: FragmentBinaryDisasmBinding? = null
    private val binding get() = _binding!!

    var handle: Int = 0
    var workerThread: Thread? = null
    private lateinit var adapter: DisasmListViewAdapter
    private lateinit var relPath: String
    private lateinit var parsedFile: AbstractFile
    private var autoSymAdapter: ArrayAdapter<String>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            relPath = it.getString(ARG_PARAM)!!
            it.clear()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBinaryDisasmBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupSymCompleteAdapter()
//        adapter = DisasmListViewAdapter(null)
        setHasOptionsMenu(true)
        val type = parsedFile.machineType // elf.header.machineType;
        val archs = getArchitecture(type)
        val arch = archs[0]
        var mode = 0
        if (archs.size == 2) mode = archs[1]
        if (arch == CS_ARCH_MAX || arch == CS_ARCH_ALL) {
            Toast.makeText(
                activity,
                "Maybe this program don't support this machine:" + type.name,
                Toast.LENGTH_SHORT
            ).show()
        } else {
//            var err: Int
            try {
                MainActivity.Open(arch, /*CS_MODE_LITTLE_ENDIAN =*/mode).also { handle = it }
                Toast.makeText(activity, "MachineType=${type.name} arch=$arch", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                Log.e(TAG, "setmode type=${type.name} err=${e}arch${arch}mode=$mode")
                Toast.makeText(
                    activity,
                    "failed to set architecture${e}arch=$arch",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        setupListView()
        disassemble()
    }

    private fun setupListView() { // moved to onCreate for avoiding NPE
        val mLayoutManager = LinearLayoutManager(context)
        adapter = DisasmListViewAdapter(parsedFile, handle, this, mLayoutManager)
        binding.disasmTabListview.adapter = adapter
        binding.disasmTabListview.layoutManager = mLayoutManager
//        adapter.addAll(disasmManager!!.getItems(), disasmManager!!.address)
        binding.disasmTabListview.addOnScrollListener(adapter.OnScrollListener())
    }

    fun disassemble() {
        Timber.v("Strted disasm")
        // NOW there's no notion of pause or resume
        workerThread = Thread {
            val codesection = parsedFile.codeSectionBase
            val start = codesection // elfUtil.getCodeSectionOffset();
//            val limit = parsedFile.codeSectionLimit
            val addr = parsedFile.codeVirtAddr // + offset
            Timber.v("code section point :" + start.toString(16))
            Timber.d("addr : " + addr.toString(16))
            // ListViewItem lvi;
// 	getFunctionNames();
//            val size = limit - start
//            val leftbytes = size
            // DisasmIterator dai = new DisasmIterator(MainActivity.this,/*mNotifyManager,mBuilder,*/adapter, size);
// IMPORTANT: un-outcomment here if it causes a bug
// adapter.setDit(dai);
            adapter.loadMore(0, addr)
            // long toresume=dai.getSome(filecontent,start,size,addr,1000000/*, disasmResults*/);
/*if(toresume<0)
					 {
					 AlertError("Failed to disassemble:"+toresume,new Exception());
					 }else{
					 disasmManager.setResumeOffsetFromCode(toresume);
					 }*/
//            disasmResults = adapter.itemList()
            // mNotifyManager.cancel(0);
            // final int len=disasmResults.size();
            // add xrefs
            activity?.runOnUiThread {
                binding.disasmTabListview.requestLayout()
                //                tab2!!.invalidate()
                Toast.makeText(activity, "done", Toast.LENGTH_SHORT).show()
            }
            Timber.v("disassembly done")
        }
        workerThread!!.start()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.jumpto -> run {
                val autocomplete = object :
                    androidx.appcompat.widget.AppCompatAutoCompleteTextView(requireActivity()) {
                    override fun enoughToFilter(): Boolean {
                        return true
                    }

                    override fun onFocusChanged(
                        focused: Boolean,
                        direction: Int,
                        previouslyFocusedRect: Rect?
                    ) {
                        super.onFocusChanged(focused, direction, previouslyFocusedRect)
                        if (focused && adapter != null) {
                            performFiltering(text, 0)
                        }
                    }
                }
                autocomplete.setAdapter<ArrayAdapter<String>>(autoSymAdapter)
                val ab = showEditDialog(
                    requireActivity(),
                    "Goto an address/symbol",
                    "Enter a hex address or a symbol",
                    autocomplete,
                    "Go",
                    DialogInterface.OnClickListener { p1, p2 ->
                        val dest = autocomplete.text.toString()
                        try {
                            val address = dest.toLong(16)
//                            jumpto(address)
                        } catch (nfe: NumberFormatException) { // not a number, lookup symbol table
                            val syms = parsedFile.exportSymbols
                            for (sym in syms) {
                                if (sym.name != null && sym.name == dest) {
                                    if (sym.type != Symbol.Type.STT_FUNC) {
                                        Toast.makeText(
                                            activity,
                                            "This is not a function.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@OnClickListener
                                    }
//                                    jumpto(sym.st_value)
                                    return@OnClickListener
                                }
                            }
                            showToast(requireActivity(), "No such symbol available")
                        }
                    },
                    getString(R.string.cancel) /*R.string.symbol*/,
                    null
                )
                ab.window?.setGravity(Gravity.TOP)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun parseAddress(toString: String?): Long {
        if (toString == null) {
            return parsedFile.entryPoint
        }
        if (toString == "") {
            return parsedFile.entryPoint
        }
        try {
            return java.lang.Long.decode(toString)
        } catch (e: NumberFormatException) {
            Toast.makeText(activity, R.string.validaddress, Toast.LENGTH_SHORT).show()
        }
        return parsedFile.entryPoint
    }


    private fun isValidAddress(address: Long): Boolean {
        return if (address > parsedFile.fileContents.size + parsedFile.codeVirtAddr) false else address >= 0
    }


    override fun onResume() {
        super.onResume()
        if (ColorHelper.isUpdatedColor) {
            binding.disasmTabListview.refreshDrawableState()
            ColorHelper.isUpdatedColor = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivity.Finalize(handle)
    }

    private fun setupSymCompleteAdapter() {
        autoSymAdapter = ArrayAdapter(requireActivity(), android.R.layout.select_dialog_item)
        // autocomplete.setThreshold(2);
        // autocomplete.setAdapter(autoSymAdapter);
    }

    companion object {
        private val ARG_PARAM: String = "relpath"
    }
}
