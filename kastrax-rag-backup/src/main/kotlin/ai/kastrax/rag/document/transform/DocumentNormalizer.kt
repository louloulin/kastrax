package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import java.text.Normalizer
import java.util.Locale

private val logger = KotlinLogging.logger {}

/**
 * 文档标准化器，用于标准化文档内容，使其格式一致。
 */
class DocumentNormalizer {

    /**
     * 标准化文档。
     *
     * @param document 要标准化的文档
     * @param options 标准化选项
     * @return 标准化后的文档
     */
    fun normalize(document: Document, options: NormalizationOptions = NormalizationOptions()): Document {
        val normalizedContent = normalize(document.content, options)
        
        // 标准化元数据
        val normalizedMetadata = if (options.normalizeMetadata) {
            normalizeMetadata(document.metadata, options)
        } else {
            document.metadata
        }
        
        return Document(normalizedContent, normalizedMetadata)
    }

    /**
     * 标准化文本内容。
     *
     * @param text 要标准化的文本
     * @param options 标准化选项
     * @return 标准化后的文本
     */
    fun normalize(text: String, options: NormalizationOptions = NormalizationOptions()): String {
        var normalizedText = text

        try {
            // Unicode 标准化
            if (options.unicodeNormalization) {
                normalizedText = normalizeUnicode(normalizedText, options.unicodeForm)
            }

            // 大小写标准化
            if (options.caseNormalization) {
                normalizedText = normalizeCase(normalizedText, options.caseForm)
            }

            // 空白字符标准化
            if (options.whitespaceNormalization) {
                normalizedText = normalizeWhitespace(normalizedText)
            }

            // 标点符号标准化
            if (options.punctuationNormalization) {
                normalizedText = normalizePunctuation(normalizedText)
            }

            // 数字标准化
            if (options.numberNormalization) {
                normalizedText = normalizeNumbers(normalizedText, options.numberFormat)
            }

            // 日期标准化
            if (options.dateNormalization) {
                normalizedText = normalizeDates(normalizedText, options.dateFormat)
            }

            // 缩写标准化
            if (options.abbreviationNormalization) {
                normalizedText = normalizeAbbreviations(normalizedText, options.abbreviations)
            }

            // 拼写标准化
            if (options.spellingNormalization) {
                normalizedText = normalizeSpelling(normalizedText, options.spellingCorrections)
            }

            // 段落标准化
            if (options.paragraphNormalization) {
                normalizedText = normalizeParagraphs(normalizedText)
            }

            // 删除控制字符
            if (options.removeControlCharacters) {
                normalizedText = removeControlCharacters(normalizedText)
            }

            return normalizedText
        } catch (e: Exception) {
            logger.error(e) { "Error normalizing text" }
            return text  // 如果标准化失败，返回原始文本
        }
    }

    /**
     * 标准化元数据。
     *
     * @param metadata 要标准化的元数据
     * @param options 标准化选项
     * @return 标准化后的元数据
     */
    private fun normalizeMetadata(metadata: Map<String, Any>, options: NormalizationOptions): Map<String, Any> {
        val normalizedMetadata = mutableMapOf<String, Any>()
        
        for ((key, value) in metadata) {
            // 标准化键名
            val normalizedKey = normalizeMetadataKey(key, options)
            
            // 标准化值
            val normalizedValue = when (value) {
                is String -> normalize(value, options)
                is List<*> -> value.map { if (it is String) normalize(it, options) else it }
                else -> value
            }
            
            normalizedMetadata[normalizedKey] = normalizedValue
        }
        
        return normalizedMetadata
    }

    /**
     * 标准化元数据键名。
     *
     * @param key 要标准化的键名
     * @param options 标准化选项
     * @return 标准化后的键名
     */
    private fun normalizeMetadataKey(key: String, options: NormalizationOptions): String {
        var normalizedKey = key
        
        // 转换为小写
        if (options.metadataKeysToLowercase) {
            normalizedKey = normalizedKey.lowercase()
        }
        
        // 替换空格为下划线
        if (options.metadataKeysReplaceSpaces) {
            normalizedKey = normalizedKey.replace("\\s+".toRegex(), "_")
        }
        
        // 移除特殊字符
        if (options.metadataKeysRemoveSpecialChars) {
            normalizedKey = normalizedKey.replace("[^a-zA-Z0-9_]".toRegex(), "")
        }
        
        return normalizedKey
    }

    /**
     * Unicode 标准化。
     *
     * @param text 要标准化的文本
     * @param form Unicode 标准化形式
     * @return 标准化后的文本
     */
    private fun normalizeUnicode(text: String, form: Normalizer.Form): String {
        return Normalizer.normalize(text, form)
    }

    /**
     * 大小写标准化。
     *
     * @param text 要标准化的文本
     * @param caseForm 大小写形式
     * @return 标准化后的文本
     */
    private fun normalizeCase(text: String, caseForm: CaseForm): String {
        return when (caseForm) {
            CaseForm.LOWERCASE -> text.lowercase()
            CaseForm.UPPERCASE -> text.uppercase()
            CaseForm.TITLE_CASE -> titleCase(text)
            CaseForm.SENTENCE_CASE -> sentenceCase(text)
            CaseForm.PRESERVE -> text
        }
    }

    /**
     * 将文本转换为标题大小写。
     *
     * @param text 要转换的文本
     * @return 转换后的文本
     */
    private fun titleCase(text: String): String {
        val words = text.split("\\s+".toRegex())
        return words.joinToString(" ") { word ->
            if (word.isEmpty()) word else word[0].uppercase() + word.substring(1).lowercase()
        }
    }

    /**
     * 将文本转换为句子大小写。
     *
     * @param text 要转换的文本
     * @return 转换后的文本
     */
    private fun sentenceCase(text: String): String {
        val sentences = text.split("(?<=[.!?])\\s+".toRegex())
        return sentences.joinToString(" ") { sentence ->
            if (sentence.isEmpty()) sentence else sentence[0].uppercase() + sentence.substring(1).lowercase()
        }
    }

    /**
     * 空白字符标准化。
     *
     * @param text 要标准化的文本
     * @return 标准化后的文本
     */
    private fun normalizeWhitespace(text: String): String {
        // 将所有空白字符（包括制表符、换行符等）替换为单个空格
        var normalized = text.replace("\\s+".toRegex(), " ")
        
        // 确保标点符号前没有空格，后有空格
        normalized = normalized.replace("\\s+([.,;:!?])".toRegex(), "$1")
        normalized = normalized.replace("([.,;:!?])(?!\\s|$)".toRegex(), "$1 ")
        
        return normalized.trim()
    }

    /**
     * 标点符号标准化。
     *
     * @param text 要标准化的文本
     * @return 标准化后的文本
     */
    private fun normalizePunctuation(text: String): String {
        var normalized = text

        // 规范化引号
        normalized = normalized.replace("[\u201c\u201d]".toRegex(), "\"")
        normalized = normalized.replace("[\u2018\u2019]".toRegex(), "'")

        // 规范化破折号
        normalized = normalized.replace("[\u2014\u2013]".toRegex(), "-")

        // 规范化省略号
        normalized = normalized.replace("\\.{2,}".toRegex(), "...")

        // 规范化连续的标点符号
        normalized = normalized.replace("\\?{2,}".toRegex(), "?")
        normalized = normalized.replace("!{2,}".toRegex(), "!")
        normalized = normalized.replace(",{2,}".toRegex(), ",")

        return normalized
    }

    /**
     * 数字标准化。
     *
     * @param text 要标准化的文本
     * @param format 数字格式
     * @return 标准化后的文本
     */
    private fun normalizeNumbers(text: String, format: NumberFormat): String {
        if (format == NumberFormat.PRESERVE) {
            return text
        }
        
        val locale = when (format) {
            NumberFormat.US -> Locale.US
            NumberFormat.EUROPEAN -> Locale.FRANCE
            NumberFormat.INTERNATIONAL -> Locale.ROOT
            else -> Locale.getDefault()
        }
        
        // 查找数字并格式化
        val numberPattern = "\\b\\d+([.,]\\d+)?\\b".toRegex()
        return text.replace(numberPattern) { matchResult ->
            try {
                val number = matchResult.value.replace(",", ".")
                val value = number.toDouble()
                
                when (format) {
                    NumberFormat.WORDS -> numberToWords(value.toLong())
                    else -> {
                        val formatter = java.text.NumberFormat.getInstance(locale)
                        formatter.format(value)
                    }
                }
            } catch (e: Exception) {
                matchResult.value
            }
        }
    }

    /**
     * 将数字转换为单词。
     *
     * @param number 要转换的数字
     * @return 转换后的单词
     */
    private fun numberToWords(number: Long): String {
        if (number == 0L) return "zero"
        
        val units = arrayOf("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                           "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
        val tens = arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
        
        fun convert(num: Long): String {
            return when {
                num < 20 -> units[num.toInt()]
                num < 100 -> tens[(num / 10).toInt()] + if (num % 10 != 0L) "-" + units[(num % 10).toInt()] else ""
                num < 1000 -> units[(num / 100).toInt()] + " hundred" + if (num % 100 != 0L) " " + convert(num % 100) else ""
                num < 1000000 -> convert(num / 1000) + " thousand" + if (num % 1000 != 0L) " " + convert(num % 1000) else ""
                num < 1000000000 -> convert(num / 1000000) + " million" + if (num % 1000000 != 0L) " " + convert(num % 1000000) else ""
                else -> convert(num / 1000000000) + " billion" + if (num % 1000000000 != 0L) " " + convert(num % 1000000000) else ""
            }
        }
        
        return convert(number)
    }

    /**
     * 日期标准化。
     *
     * @param text 要标准化的文本
     * @param format 日期格式
     * @return 标准化后的文本
     */
    private fun normalizeDates(text: String, format: String): String {
        // 匹配常见的日期格式
        val datePatterns = listOf(
            "\\b(\\d{1,2})/(\\d{1,2})/(\\d{2,4})\\b".toRegex(),  // MM/DD/YYYY or DD/MM/YYYY
            "\\b(\\d{1,2})-(\\d{1,2})-(\\d{2,4})\\b".toRegex(),  // MM-DD-YYYY or DD-MM-YYYY
            "\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\b".toRegex(),    // YYYY-MM-DD
            "\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* (\\d{1,2}),? (\\d{4})\\b".toRegex(RegexOption.IGNORE_CASE)  // Month DD, YYYY
        )
        
        var result = text
        
        for (pattern in datePatterns) {
            result = result.replace(pattern) { matchResult ->
                try {
                    // 解析日期
                    val date = when (pattern) {
                        datePatterns[0], datePatterns[1] -> {
                            val month = matchResult.groupValues[1].toInt()
                            val day = matchResult.groupValues[2].toInt()
                            val year = matchResult.groupValues[3].toInt().let {
                                if (it < 100) it + if (it < 50) 2000 else 1900 else it
                            }
                            java.time.LocalDate.of(year, month, day)
                        }
                        datePatterns[2] -> {
                            val year = matchResult.groupValues[1].toInt()
                            val month = matchResult.groupValues[2].toInt()
                            val day = matchResult.groupValues[3].toInt()
                            java.time.LocalDate.of(year, month, day)
                        }
                        else -> {
                            val monthStr = matchResult.groupValues[1]
                            val day = matchResult.groupValues[2].toInt()
                            val year = matchResult.groupValues[3].toInt()
                            val month = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
                                .indexOf(monthStr.take(3).lowercase()) + 1
                            java.time.LocalDate.of(year, month, day)
                        }
                    }
                    
                    // 格式化日期
                    val formatter = java.time.format.DateTimeFormatter.ofPattern(format)
                    date.format(formatter)
                } catch (e: Exception) {
                    matchResult.value
                }
            }
        }
        
        return result
    }

    /**
     * 缩写标准化。
     *
     * @param text 要标准化的文本
     * @param abbreviations 缩写映射
     * @return 标准化后的文本
     */
    private fun normalizeAbbreviations(text: String, abbreviations: Map<String, String>): String {
        var result = text
        
        for ((abbr, full) in abbreviations) {
            val pattern = "\\b$abbr\\b".toRegex(RegexOption.IGNORE_CASE)
            result = result.replace(pattern, full)
        }
        
        return result
    }

    /**
     * 拼写标准化。
     *
     * @param text 要标准化的文本
     * @param corrections 拼写更正映射
     * @return 标准化后的文本
     */
    private fun normalizeSpelling(text: String, corrections: Map<String, String>): String {
        var result = text
        
        for ((misspelled, correct) in corrections) {
            val pattern = "\\b$misspelled\\b".toRegex(RegexOption.IGNORE_CASE)
            result = result.replace(pattern, correct)
        }
        
        return result
    }

    /**
     * 段落标准化。
     *
     * @param text 要标准化的文本
     * @return 标准化后的文本
     */
    private fun normalizeParagraphs(text: String): String {
        // 将多个空行替换为单个空行
        var result = text.replace("\n{3,}".toRegex(), "\n\n")
        
        // 确保段落之间有一个空行
        result = result.replace("(?<!\n)\n(?!\n)".toRegex(), "\n\n")
        
        return result
    }

    /**
     * 删除控制字符。
     *
     * @param text 要处理的文本
     * @return 处理后的文本
     */
    private fun removeControlCharacters(text: String): String {
        return text.replace("[\\p{Cc}&&[^\n\r\t]]".toRegex(), "")
    }

    /**
     * 大小写形式。
     */
    enum class CaseForm {
        /**
         * 小写。
         */
        LOWERCASE,
        
        /**
         * 大写。
         */
        UPPERCASE,
        
        /**
         * 标题大小写（每个单词首字母大写）。
         */
        TITLE_CASE,
        
        /**
         * 句子大小写（每个句子首字母大写）。
         */
        SENTENCE_CASE,
        
        /**
         * 保留原始大小写。
         */
        PRESERVE
    }

    /**
     * 数字格式。
     */
    enum class NumberFormat {
        /**
         * 美国格式（例如：1,234.56）。
         */
        US,
        
        /**
         * 欧洲格式（例如：1.234,56）。
         */
        EUROPEAN,
        
        /**
         * 国际格式（例如：1234.56）。
         */
        INTERNATIONAL,
        
        /**
         * 单词形式（例如：one thousand two hundred thirty-four）。
         */
        WORDS,
        
        /**
         * 保留原始格式。
         */
        PRESERVE
    }

    /**
     * 文档标准化选项。
     *
     * @property unicodeNormalization 是否进行 Unicode 标准化
     * @property unicodeForm Unicode 标准化形式
     * @property caseNormalization 是否进行大小写标准化
     * @property caseForm 大小写形式
     * @property whitespaceNormalization 是否进行空白字符标准化
     * @property punctuationNormalization 是否进行标点符号标准化
     * @property numberNormalization 是否进行数字标准化
     * @property numberFormat 数字格式
     * @property dateNormalization 是否进行日期标准化
     * @property dateFormat 日期格式
     * @property abbreviationNormalization 是否进行缩写标准化
     * @property abbreviations 缩写映射
     * @property spellingNormalization 是否进行拼写标准化
     * @property spellingCorrections 拼写更正映射
     * @property paragraphNormalization 是否进行段落标准化
     * @property removeControlCharacters 是否删除控制字符
     * @property normalizeMetadata 是否标准化元数据
     * @property metadataKeysToLowercase 是否将元数据键名转换为小写
     * @property metadataKeysReplaceSpaces 是否将元数据键名中的空格替换为下划线
     * @property metadataKeysRemoveSpecialChars 是否移除元数据键名中的特殊字符
     */
    data class NormalizationOptions(
        val unicodeNormalization: Boolean = true,
        val unicodeForm: Normalizer.Form = Normalizer.Form.NFC,
        val caseNormalization: Boolean = false,
        val caseForm: CaseForm = CaseForm.PRESERVE,
        val whitespaceNormalization: Boolean = true,
        val punctuationNormalization: Boolean = true,
        val numberNormalization: Boolean = false,
        val numberFormat: NumberFormat = NumberFormat.PRESERVE,
        val dateNormalization: Boolean = false,
        val dateFormat: String = "yyyy-MM-dd",
        val abbreviationNormalization: Boolean = false,
        val abbreviations: Map<String, String> = DEFAULT_ABBREVIATIONS,
        val spellingNormalization: Boolean = false,
        val spellingCorrections: Map<String, String> = DEFAULT_SPELLING_CORRECTIONS,
        val paragraphNormalization: Boolean = true,
        val removeControlCharacters: Boolean = true,
        val normalizeMetadata: Boolean = true,
        val metadataKeysToLowercase: Boolean = true,
        val metadataKeysReplaceSpaces: Boolean = true,
        val metadataKeysRemoveSpecialChars: Boolean = false
    ) {
        /**
         * 创建一个新的 NormalizationOptions 构建器。
         *
         * @return NormalizationOptionsBuilder 实例
         */
        fun toBuilder(): NormalizationOptionsBuilder {
            return NormalizationOptionsBuilder(this)
        }

        /**
         * NormalizationOptions 构建器，用于链式配置标准化选项。
         */
        class NormalizationOptionsBuilder(options: NormalizationOptions) {
            private var unicodeNormalization = options.unicodeNormalization
            private var unicodeForm = options.unicodeForm
            private var caseNormalization = options.caseNormalization
            private var caseForm = options.caseForm
            private var whitespaceNormalization = options.whitespaceNormalization
            private var punctuationNormalization = options.punctuationNormalization
            private var numberNormalization = options.numberNormalization
            private var numberFormat = options.numberFormat
            private var dateNormalization = options.dateNormalization
            private var dateFormat = options.dateFormat
            private var abbreviationNormalization = options.abbreviationNormalization
            private var abbreviations = options.abbreviations
            private var spellingNormalization = options.spellingNormalization
            private var spellingCorrections = options.spellingCorrections
            private var paragraphNormalization = options.paragraphNormalization
            private var removeControlCharacters = options.removeControlCharacters
            private var normalizeMetadata = options.normalizeMetadata
            private var metadataKeysToLowercase = options.metadataKeysToLowercase
            private var metadataKeysReplaceSpaces = options.metadataKeysReplaceSpaces
            private var metadataKeysRemoveSpecialChars = options.metadataKeysRemoveSpecialChars

            fun unicodeNormalization(value: Boolean) = apply { unicodeNormalization = value }
            fun unicodeForm(value: Normalizer.Form) = apply { unicodeForm = value }
            fun caseNormalization(value: Boolean) = apply { caseNormalization = value }
            fun caseForm(value: CaseForm) = apply { caseForm = value }
            fun whitespaceNormalization(value: Boolean) = apply { whitespaceNormalization = value }
            fun punctuationNormalization(value: Boolean) = apply { punctuationNormalization = value }
            fun numberNormalization(value: Boolean) = apply { numberNormalization = value }
            fun numberFormat(value: NumberFormat) = apply { numberFormat = value }
            fun dateNormalization(value: Boolean) = apply { dateNormalization = value }
            fun dateFormat(value: String) = apply { dateFormat = value }
            fun abbreviationNormalization(value: Boolean) = apply { abbreviationNormalization = value }
            fun abbreviations(value: Map<String, String>) = apply { abbreviations = value }
            fun spellingNormalization(value: Boolean) = apply { spellingNormalization = value }
            fun spellingCorrections(value: Map<String, String>) = apply { spellingCorrections = value }
            fun paragraphNormalization(value: Boolean) = apply { paragraphNormalization = value }
            fun removeControlCharacters(value: Boolean) = apply { removeControlCharacters = value }
            fun normalizeMetadata(value: Boolean) = apply { normalizeMetadata = value }
            fun metadataKeysToLowercase(value: Boolean) = apply { metadataKeysToLowercase = value }
            fun metadataKeysReplaceSpaces(value: Boolean) = apply { metadataKeysReplaceSpaces = value }
            fun metadataKeysRemoveSpecialChars(value: Boolean) = apply { metadataKeysRemoveSpecialChars = value }

            fun build(): NormalizationOptions {
                return NormalizationOptions(
                    unicodeNormalization = unicodeNormalization,
                    unicodeForm = unicodeForm,
                    caseNormalization = caseNormalization,
                    caseForm = caseForm,
                    whitespaceNormalization = whitespaceNormalization,
                    punctuationNormalization = punctuationNormalization,
                    numberNormalization = numberNormalization,
                    numberFormat = numberFormat,
                    dateNormalization = dateNormalization,
                    dateFormat = dateFormat,
                    abbreviationNormalization = abbreviationNormalization,
                    abbreviations = abbreviations,
                    spellingNormalization = spellingNormalization,
                    spellingCorrections = spellingCorrections,
                    paragraphNormalization = paragraphNormalization,
                    removeControlCharacters = removeControlCharacters,
                    normalizeMetadata = normalizeMetadata,
                    metadataKeysToLowercase = metadataKeysToLowercase,
                    metadataKeysReplaceSpaces = metadataKeysReplaceSpaces,
                    metadataKeysRemoveSpecialChars = metadataKeysRemoveSpecialChars
                )
            }
        }
    }

    companion object {
        /**
         * 默认缩写映射。
         */
        val DEFAULT_ABBREVIATIONS = mapOf(
            "Dr." to "Doctor",
            "Mr." to "Mister",
            "Mrs." to "Mistress",
            "Ms." to "Miss",
            "Prof." to "Professor",
            "e.g." to "for example",
            "i.e." to "that is",
            "etc." to "et cetera",
            "vs." to "versus",
            "approx." to "approximately",
            "dept." to "department",
            "govt." to "government"
        )

        /**
         * 默认拼写更正映射。
         */
        val DEFAULT_SPELLING_CORRECTIONS = mapOf(
            "teh" to "the",
            "recieve" to "receive",
            "definately" to "definitely",
            "seperate" to "separate",
            "occured" to "occurred",
            "untill" to "until",
            "wich" to "which",
            "thier" to "their",
            "recieved" to "received",
            "accomodate" to "accommodate"
        )

        /**
         * 创建一个预设的标准化选项，用于基本标准化。
         *
         * @return 基本标准化选项
         */
        fun basicNormalizationOptions(): NormalizationOptions {
            return NormalizationOptions(
                unicodeNormalization = true,
                whitespaceNormalization = true,
                punctuationNormalization = true,
                paragraphNormalization = true,
                removeControlCharacters = true
            )
        }

        /**
         * 创建一个预设的标准化选项，用于高级标准化。
         *
         * @return 高级标准化选项
         */
        fun advancedNormalizationOptions(): NormalizationOptions {
            return NormalizationOptions(
                unicodeNormalization = true,
                caseNormalization = true,
                caseForm = CaseForm.LOWERCASE,
                whitespaceNormalization = true,
                punctuationNormalization = true,
                numberNormalization = true,
                numberFormat = NumberFormat.INTERNATIONAL,
                dateNormalization = true,
                dateFormat = "yyyy-MM-dd",
                abbreviationNormalization = true,
                spellingNormalization = true,
                paragraphNormalization = true,
                removeControlCharacters = true
            )
        }

        /**
         * 创建一个预设的标准化选项，用于搜索引擎优化。
         *
         * @return 搜索引擎优化标准化选项
         */
        fun seoNormalizationOptions(): NormalizationOptions {
            return NormalizationOptions(
                unicodeNormalization = true,
                caseNormalization = true,
                caseForm = CaseForm.LOWERCASE,
                whitespaceNormalization = true,
                punctuationNormalization = true,
                numberNormalization = false,
                dateNormalization = false,
                abbreviationNormalization = true,
                spellingNormalization = true,
                paragraphNormalization = true,
                removeControlCharacters = true
            )
        }
    }
}
