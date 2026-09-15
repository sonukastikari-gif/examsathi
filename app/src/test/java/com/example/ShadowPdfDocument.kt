package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.OutputStream
import java.lang.reflect.Constructor

@Implements(PdfDocument::class)
class ShadowPdfDocument {

    private var isClosed = false

    @Implementation
    fun __constructor__() {
        isClosed = false
    }

    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        val bitmap = Bitmap.createBitmap(pageInfo.pageWidth, pageInfo.pageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Find Page constructor
        val constructors = PdfDocument.Page::class.java.declaredConstructors
        val constructor = constructors.first { it.parameterTypes.size == 2 } as Constructor<PdfDocument.Page>
        constructor.isAccessible = true
        return constructor.newInstance(canvas, pageInfo)
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {
        // no-op for shadow
    }

    @Implementation
    fun writeTo(out: OutputStream) {
        out.write("%PDF-1.4\n%ExamSathi Official Scorecard Report\n%%EOF\n".toByteArray(Charsets.UTF_8))
    }

    @Implementation
    fun close() {
        isClosed = true
    }
}
