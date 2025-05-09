package ai.kastrax.rag.document

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat

private val logger = KotlinLogging.logger {}

/**
 * Excel文档加载器，从Excel文件（.xls或.xlsx）加载文档。
 *
 * @property file 要加载的Excel文件
 * @property metadata 要添加到文档的元数据
 * @property sheetNames 要加载的工作表名称列表，默认为null（加载所有工作表）
 * @property sheetIndices 要加载的工作表索引列表，默认为null（加载所有工作表）
 * @property documentPerSheet 是否为每个工作表创建一个文档，默认为false（整个Excel作为一个文档）
 * @property documentPerRow 是否为每行创建一个文档，默认为false
 * @property hasHeaderRow 是否有标题行，默认为true
 * @property includeHeaderInContent 是否在内容中包含标题，默认为true
 * @property columnDelimiter 列之间的分隔符，用于构建文本内容，默认为"\t"
 * @property rowDelimiter 行之间的分隔符，用于构建文本内容，默认为"\n"
 * @property sheetDelimiter 工作表之间的分隔符，用于构建文本内容，默认为"\n\n"
 * @property dateFormat 日期格式，默认为"yyyy-MM-dd"
 */
class ExcelDocumentLoader(
    private val file: File,
    private val metadata: Map<String, Any> = emptyMap(),
    private val sheetNames: List<String>? = null,
    private val sheetIndices: List<Int>? = null,
    private val documentPerSheet: Boolean = false,
    private val documentPerRow: Boolean = false,
    private val hasHeaderRow: Boolean = true,
    private val includeHeaderInContent: Boolean = true,
    private val columnDelimiter: String = "\t",
    private val rowDelimiter: String = "\n",
    private val sheetDelimiter: String = "\n\n",
    private val dateFormat: String = "yyyy-MM-dd"
) : DocumentLoader {

    constructor(
        filePath: String,
        metadata: Map<String, Any> = emptyMap(),
        sheetNames: List<String>? = null,
        sheetIndices: List<Int>? = null,
        documentPerSheet: Boolean = false,
        documentPerRow: Boolean = false,
        hasHeaderRow: Boolean = true,
        includeHeaderInContent: Boolean = true,
        columnDelimiter: String = "\t",
        rowDelimiter: String = "\n",
        sheetDelimiter: String = "\n\n",
        dateFormat: String = "yyyy-MM-dd"
    ) : this(
        File(filePath), metadata, sheetNames, sheetIndices, documentPerSheet, documentPerRow,
        hasHeaderRow, includeHeaderInContent, columnDelimiter, rowDelimiter, sheetDelimiter, dateFormat
    )

    constructor(
        inputStream: InputStream,
        metadata: Map<String, Any> = emptyMap(),
        sheetNames: List<String>? = null,
        sheetIndices: List<Int>? = null,
        documentPerSheet: Boolean = false,
        documentPerRow: Boolean = false,
        hasHeaderRow: Boolean = true,
        includeHeaderInContent: Boolean = true,
        columnDelimiter: String = "\t",
        rowDelimiter: String = "\n",
        sheetDelimiter: String = "\n\n",
        dateFormat: String = "yyyy-MM-dd"
    ) : this(
        createTempFile(inputStream),
        metadata, sheetNames, sheetIndices, documentPerSheet, documentPerRow,
        hasHeaderRow, includeHeaderInContent, columnDelimiter, rowDelimiter, sheetDelimiter, dateFormat
    )

    override suspend fun load(): List<Document> {
        logger.debug { "Loading Excel file: ${file.absolutePath}" }

        if (!file.exists()) {
            logger.error { "File does not exist: ${file.absolutePath}" }
            return emptyList()
        }

        if (!file.isFile) {
            logger.error { "Path is not a file: ${file.absolutePath}" }
            return emptyList()
        }

        return try {
            WorkbookFactory.create(file).use { workbook ->
                // 基础元数据
                val baseMetadata = mutableMapOf(
                    "source" to file.absolutePath,
                    "file_name" to file.name,
                    "file_extension" to file.extension,
                    "file_size" to file.length(),
                    "file_last_modified" to file.lastModified(),
                    "sheet_count" to workbook.numberOfSheets
                )
                
                // 合并用户提供的元数据
                val fullMetadata = baseMetadata + metadata
                
                // 获取要处理的工作表
                val sheets = getSheets(workbook)
                
                when {
                    documentPerRow -> {
                        // 为每行创建一个文档
                        val documents = mutableListOf<Document>()
                        
                        sheets.forEach { sheet ->
                            val headers = if (hasHeaderRow && sheet.physicalNumberOfRows > 0) {
                                getRowValues(sheet.getRow(0))
                            } else {
                                emptyList()
                            }
                            
                            val startRow = if (hasHeaderRow) 1 else 0
                            
                            for (rowIndex in startRow until sheet.physicalNumberOfRows) {
                                val row = sheet.getRow(rowIndex) ?: continue
                                val rowContent = buildRowContent(row, headers)
                                
                                val rowMetadata = fullMetadata + mapOf(
                                    "sheet_name" to sheet.sheetName,
                                    "sheet_index" to workbook.getSheetIndex(sheet),
                                    "row_index" to rowIndex,
                                    "row_number" to (rowIndex + 1)
                                )
                                
                                documents.add(Document(rowContent, rowMetadata))
                            }
                        }
                        
                        documents
                    }
                    documentPerSheet -> {
                        // 为每个工作表创建一个文档
                        sheets.map { sheet ->
                            val sheetContent = buildSheetContent(sheet)
                            
                            val sheetMetadata = fullMetadata + mapOf(
                                "sheet_name" to sheet.sheetName,
                                "sheet_index" to workbook.getSheetIndex(sheet),
                                "row_count" to sheet.physicalNumberOfRows
                            )
                            
                            Document(sheetContent, sheetMetadata)
                        }
                    }
                    else -> {
                        // 整个Excel作为一个文档
                        val content = buildWorkbookContent(sheets)
                        
                        val sheetNames = sheets.map { it.sheetName }
                        val excelMetadata = fullMetadata + mapOf(
                            "sheet_names" to sheetNames
                        )
                        
                        listOf(Document(content, excelMetadata))
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading Excel file: ${file.absolutePath}" }
            emptyList()
        }
    }

    /**
     * 获取要处理的工作表。
     */
    private fun getSheets(workbook: Workbook): List<Sheet> {
        return when {
            sheetNames != null -> {
                sheetNames.mapNotNull { name ->
                    workbook.getSheet(name)
                }
            }
            sheetIndices != null -> {
                sheetIndices.mapNotNull { index ->
                    if (index >= 0 && index < workbook.numberOfSheets) {
                        workbook.getSheetAt(index)
                    } else {
                        null
                    }
                }
            }
            else -> {
                (0 until workbook.numberOfSheets).map { workbook.getSheetAt(it) }
            }
        }
    }

    /**
     * 获取行中的所有单元格值。
     */
    private fun getRowValues(row: Row?): List<String> {
        if (row == null) return emptyList()
        
        return (0 until row.lastCellNum).map { cellIndex ->
            val cell = row.getCell(cellIndex)
            getCellValue(cell)
        }
    }

    /**
     * 获取单元格的值。
     */
    private fun getCellValue(cell: Cell?): String {
        if (cell == null) return ""
        
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val dateFormat = SimpleDateFormat(this.dateFormat)
                    dateFormat.format(cell.dateCellValue)
                } else {
                    cell.numericCellValue.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    when (cell.cachedFormulaResultType) {
                        CellType.STRING -> cell.stringCellValue
                        CellType.NUMERIC -> {
                            if (DateUtil.isCellDateFormatted(cell)) {
                                val dateFormat = SimpleDateFormat(this.dateFormat)
                                dateFormat.format(cell.dateCellValue)
                            } else {
                                cell.numericCellValue.toString()
                            }
                        }
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        else -> ""
                    }
                } catch (e: Exception) {
                    cell.cellFormula
                }
            }
            else -> ""
        }
    }

    /**
     * 为单行构建内容。
     */
    private fun buildRowContent(row: Row, headers: List<String>): String {
        val values = getRowValues(row)
        
        return if (headers.isNotEmpty()) {
            // 使用标题构建内容
            headers.zip(values.padEnd(headers.size, "")) { header, value ->
                "$header: $value"
            }.joinToString(rowDelimiter)
        } else {
            // 没有标题，只使用值
            values.joinToString(columnDelimiter)
        }
    }

    /**
     * 为工作表构建内容。
     */
    private fun buildSheetContent(sheet: Sheet): String {
        val builder = StringBuilder()
        builder.append("Sheet: ${sheet.sheetName}").append(rowDelimiter)
        
        if (sheet.physicalNumberOfRows > 0) {
            val headers = if (hasHeaderRow) {
                getRowValues(sheet.getRow(0))
            } else {
                emptyList()
            }
            
            val startRow = if (hasHeaderRow && includeHeaderInContent) 0 else if (hasHeaderRow) 1 else 0
            
            for (rowIndex in startRow until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(rowIndex) ?: continue
                val values = getRowValues(row)
                
                if (rowIndex == 0 && hasHeaderRow && includeHeaderInContent) {
                    // 这是标题行
                    builder.append(values.joinToString(columnDelimiter))
                } else {
                    // 这是数据行
                    if (headers.isNotEmpty() && hasHeaderRow) {
                        // 使用标题构建行内容
                        val rowContent = headers.zip(values.padEnd(headers.size, "")) { header, value ->
                            "$header: $value"
                        }.joinToString(columnDelimiter)
                        builder.append(rowContent)
                    } else {
                        // 没有标题，只使用值
                        builder.append(values.joinToString(columnDelimiter))
                    }
                }
                
                if (rowIndex < sheet.physicalNumberOfRows - 1) {
                    builder.append(rowDelimiter)
                }
            }
        }
        
        return builder.toString()
    }

    /**
     * 为整个工作簿构建内容。
     */
    private fun buildWorkbookContent(sheets: List<Sheet>): String {
        return sheets.joinToString(sheetDelimiter) { sheet ->
            buildSheetContent(sheet)
        }
    }

    companion object {
        /**
         * 从输入流创建临时文件。
         */
        private fun createTempFile(inputStream: InputStream): File {
            val tempFile = File.createTempFile("excel_", ".xlsx")
            tempFile.deleteOnExit()
            
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            return tempFile
        }
        
        /**
         * 扩展函数：将列表填充到指定长度。
         */
        private fun <T> List<T>.padEnd(length: Int, defaultValue: T): List<T> {
            return if (size >= length) {
                this
            } else {
                this + List(length - size) { defaultValue }
            }
        }
    }
}
