package ai.kastrax.codebase.actor

/**
 * Actor 属性
 *
 * @property producer Actor 生产者
 */
data class Props(val producer: () -> Actor) {
    companion object {
        /**
         * 从生产者创建 Props
         *
         * @param producer Actor 生产者
         * @return Props
         */
        fun fromProducer(producer: () -> Actor): Props {
            return Props(producer)
        }
    }
}
