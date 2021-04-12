package com.example.consumerapp

import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.picodiploma.mynotesapp.adapter.NoteAdapter
import com.dicoding.picodiploma.mynotesapp.db.DatabaseContract.NoteColumns.Companion.CONTENT_URI
import com.dicoding.picodiploma.mynotesapp.entity.Note
import com.dicoding.picodiploma.mynotesapp.helper.MappingHelper
import com.example.consumerapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: NoteAdapter

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val EXTRA_STATE = "EXTRA_STATE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Customer Notes"

        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.setHasFixedSize(true)
        adapter = NoteAdapter(this)
        binding.rvNotes.adapter = adapter

        binding.fabAdd.setOnClickListener {
            val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
//            startActivityForResult(intent, NoteAddUpdateActivity.REQUEST_ADD)
            startActivity(intent)
        }

        val handlerThread = HandlerThread("DataObserver")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)

        val myObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                loadNotesAsync()
                Log.d("asdasd", "Di panggil di onchange ContentObserver")
            }
        }

        contentResolver.registerContentObserver(CONTENT_URI, true, myObserver)

        if (savedInstanceState == null) {
            loadNotesAsync()
        } else {
            val list = savedInstanceState.getParcelableArrayList<Note>(EXTRA_STATE)
            if (list != null) {
                adapter.listNotes = list
            }
        }
    }

    private fun loadNotesAsync() {
        GlobalScope.launch(Dispatchers.Main) {
            binding.progressbar.visibility = View.VISIBLE
//            val noteHelper = NoteHelper.getInstance(applicationContext)
//            noteHelper.open()
            val deferredNotes = async(Dispatchers.IO) {
                // CONTENT_URI = content://com.dicoding.picodiploma.mynotesapp/note
//                val cursor = noteHelper.queryAll()
                val cursor = contentResolver.query(CONTENT_URI, null, null, null, null)
                MappingHelper.mapCursorToArrayList(cursor)
            }
            binding.progressbar.visibility = View.INVISIBLE
            val notes = deferredNotes.await()
//            noteHelper.close()
            if (notes.size > 0) {
                adapter.listNotes = notes
            } else {
                adapter.listNotes = ArrayList()
                showSnackbarMessage("Tidak ada data saat ini")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(EXTRA_STATE, adapter.listNotes)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (data != null) {
//            when (requestCode) {
//                // Akan dipanggil jika request codenya ADD
//                NoteAddUpdateActivity.REQUEST_ADD -> if (resultCode == NoteAddUpdateActivity.RESULT_ADD) {
//                    val note = data.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE) as Note
//
//                    adapter.addItem(note)
//                    binding.rvNotes.smoothScrollToPosition(adapter.itemCount - 1)
//
//                    showSnackbarMessage("Satu item berhasil ditambahkan")
//                }
//                // Update dan Delete memiliki request code sama akan tetapi result codenya berbeda
//                NoteAddUpdateActivity.REQUEST_UPDATE ->
//                    when (resultCode) {
//                        /*
//                        Akan dipanggil jika result codenya  UPDATE
//                        Semua data di load kembali dari awal
//                        */
//                        NoteAddUpdateActivity.RESULT_UPDATE -> {
//
//                            val note = data.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE) as Note
//                            val position = data.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0)
//
//                            adapter.updateItem(position, note)
//                            binding.rvNotes.smoothScrollToPosition(position)
//
//                            showSnackbarMessage("Satu item berhasil diubah")
//                        }
//                        /*
//                        Akan dipanggil jika result codenya DELETE
//                        Delete akan menghapus data dari list berdasarkan dari position
//                        */
//                        NoteAddUpdateActivity.RESULT_DELETE -> {
//                            val position = data.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0)
//
//                            adapter.removeItem(position)
//
//                            showSnackbarMessage("Satu item berhasil dihapus")
//                        }
//                    }
//            }
//        }
//    }

    /**
     * Tampilkan snackbar
     *
     * @param message inputan message
     */
    private fun showSnackbarMessage(message: String) {
        Snackbar.make(binding.rvNotes, message, Snackbar.LENGTH_SHORT).show()
    }
}
