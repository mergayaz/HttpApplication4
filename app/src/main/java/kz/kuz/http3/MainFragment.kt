package kz.kuz.http3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class MainFragment : Fragment() {
    private lateinit var mRecyclerView: RecyclerView
    private val items: MutableList<String> = ArrayList()
    lateinit var mView: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        // устанавливается удержание фрагмента, чтобы поворот не приводил к повторному созданию
        // потока
    }

    // здесь выполнение задание по реализации бесконечной прокрутки RecyclerView по достижении его
    // конца
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        activity?.setTitle(R.string.toolbar_title)
        mView = inflater.inflate(R.layout.fragment_main, container, false)
        mRecyclerView = mView.findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = GridLayoutManager(activity, 7)
        DownloadData().dowloadData
        if (isAdded) { // проверяет, что фрагмент подключён к активности,
            // в этом случае getActivity() будет отличен от null
            // (необходимости в нём нет, добавлен для примера)
            mRecyclerView.adapter = MainAdapter(items)
            mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val linearLayoutManager = recyclerView
                            .layoutManager as LinearLayoutManager?
                    val lastVisibleItem = linearLayoutManager?.findLastVisibleItemPosition()
                    if (lastVisibleItem == 433) {
//                        Toast.makeText(activity, "That's all!!!", Toast.LENGTH_SHORT).show()
                        DownloadData().dowloadData
                    }
                }
            })
        }
        return mView
    }

    private inner class MainHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView as TextView
        fun bind(item: String?) {
            textView.text = item
        }

    }

    private inner class MainAdapter(private val mItems: List<String>) : RecyclerView.Adapter<MainHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainHolder {
            val layoutInflater = LayoutInflater.from(activity)
            val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent,
                    false)
            return MainHolder(view)
        }

        override fun onBindViewHolder(holder: MainHolder, position: Int) {
            val item = mItems[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int {
            return mItems.size
        }
    }

    private inner class DownloadData {
        var executorService: ExecutorService = Executors.newSingleThreadExecutor()
        var future: Future<*> = executorService.submit {
            try {
                val url = URL("https://kuz.kz/astana.txt")
                val connection = url.openConnection() as HttpURLConnection
                val out = ByteArrayOutputStream()
                val `in` = connection.inputStream
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException(connection.responseMessage +
                            ": with https://kuz.kz/astana.txt")
                }
                var bytesRead: Int
                val buffer = ByteArray(1024)
                while (true) {
                    if (`in`.read(buffer).also { bytesRead = it } <= 0) break
                    out.write(buffer, 0, bytesRead)
                }
                out.close()
                connection.disconnect()
                val urlBytes = out.toByteArray()
                val urlString = String(urlBytes)
                val urlLines = urlString.split("\n").toTypedArray()
                var j: Int
                for (i in 0..499999999) {
                    j = i
                }
                for (urlLine in urlLines) {
                    val urlOneLine = urlLine.split(" ").toTypedArray()
                    for (item in urlOneLine) {
                        items.add(item)
                    }
                }
                items.removeLast()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val dowloadData: Unit
            get() {
                executorService.shutdown()
                try {
                    future.get() // данная команда необходима для завершения параллельного потока
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mRecyclerView.adapter = MainAdapter(items)
                mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager?
                        val lastVisibleItem = linearLayoutManager?.findLastVisibleItemPosition()
                        // по достижении конца списка заново выполняем getDownloadData
                        if (lastVisibleItem == items.size - 1) {
                            DownloadData().dowloadData
                        }
                    }
                })
                // если это не первый экран, то делаем перемещение на позицию (длина списка) минус 434
                // (первоначальный список) минус 55 (число строк на экране)
                if (items.size > 434) {
                    mRecyclerView.layoutManager?.scrollToPosition(items.size - 434 - 55)
                }
            }
    }
}