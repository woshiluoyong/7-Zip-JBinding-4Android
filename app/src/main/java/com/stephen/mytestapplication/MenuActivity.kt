package com.stephen.mytestapplication

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IArchiveOpenCallback
import net.sf.sevenzipjbinding.ICryptoGetTextPassword
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.RandomAccessFile

class MenuActivity : AppCompatActivity() {

    private inner class OpenCallback(private val password: String?) : IArchiveOpenCallback, ICryptoGetTextPassword {
        override fun setCompleted(files: Long?, bytes: Long?) {}
        override fun setTotal(files: Long?, bytes: Long?) {}
        override fun cryptoGetTextPassword(): String {
            println("=========================>7z cryptoGetTextPassword2")
            return password ?: ""//压缩包有多个文件时才会走这里
        }
    }

    private inner class ExtractCallback(private val inArchive: IInArchive, private val dstDir: File, private val password: String?)
        : IArchiveExtractCallback, ICryptoGetTextPassword {
            private var uos: OutputStream? = null
            private var totalSize: Long = 0

            override fun setOperationResult(extractOperationResult: ExtractOperationResult?) {
                println("=========================>7z setOperationResult extractOperationResult:${extractOperationResult}")
                if(ExtractOperationResult.OK == extractOperationResult)try { uos?.close() } catch (e: Exception) { e.printStackTrace() }
            }

            override fun getStream(index: Int, extractAskMode: ExtractAskMode?): ISequentialOutStream {
                println("=========================>7z getStream index:${index} extractAskMode:${extractAskMode}")
                val isDir = inArchive.getProperty(index, PropID.IS_FOLDER) as Boolean
                try {
                    val unpackedFile = File(dstDir.path, inArchive.getStringProperty(index, PropID.PATH))
                    if (isDir) {
                        unpackedFile.mkdir()
                    } else {
                        val dir = unpackedFile.parent?.let { File(it) }
                        if (dir != null && !dir.isDirectory) dir.mkdirs()
                        unpackedFile.createNewFile()
                        uos = FileOutputStream(unpackedFile)

                    }
                } catch (e: Exception) { e.printStackTrace() }
                return ISequentialOutStream { data: ByteArray ->
                    try { if (!isDir) uos?.write(data) } catch (e: Exception) { e.printStackTrace() }
                    data.size
                }
            }

            override fun prepareOperation(extractAskMode: ExtractAskMode?) {
                println("=========================>7z prepareOperation extractAskMode:${extractAskMode}")
            }

            override fun setCompleted(complete: Long) {
                val progress = ((complete.toDouble() / totalSize) * 100).toInt()
                println("=========================>7z progress:${progress}")
            }

            override fun setTotal(total: Long) {
                totalSize = total
                println("=========================>7z total:${total}")
            }

            override fun cryptoGetTextPassword(): String {
                println("=========================>7z cryptoGetTextPassword")
                return password ?: ""
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.test7zBtn).setOnClickListener {
            println("=========================>7z start")
            val file = File(cacheDir, "cod.7z")//
            val password = "123456"//"82b3c8qkuf6986es34fwdw3qsr923fd8bc"//
            val destinationDir = cacheDir

            var inStream: RandomAccessFileInStream? = null
            var inArchive: IInArchive? = null
            try {
                if(!destinationDir.exists())destinationDir.mkdir()
                inStream = RandomAccessFileInStream(RandomAccessFile(file, "r"))
                inArchive = SevenZip.openInArchive(null, inStream, OpenCallback(password))
                inArchive.extract(null, false, ExtractCallback(inArchive, destinationDir, password))
                println("=========================>7z 完成:${inArchive.numberOfItems.toLong()}")
            } catch (e: Exception) {
                e.printStackTrace()
                println("=========================>7z err ${e.message}")
            } finally {
                inArchive?.close()
                inStream?.close()
            }
        }
    }
}