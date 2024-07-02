package com.akbas.orm.entities

import com.akbas.orm.support.ConstructorParameter

class EntitySqlParamGenerator<T : Any>(
    private val constructorParameters: List<ConstructorParameter>,
    private val idParam: ConstructorParameter
) {

    private val paramsWithoutId = constructorParameters.filter { it.name != idParam.name }

    fun toInsertParam(instance: T, excludeId: Boolean): Array<Any?> {
        val params = if (!excludeId) constructorParameters else paramsWithoutId
        val resultArray = Array<Any?>(params.size) { null }
        params.forEachIndexed { i, param ->
            val value = param.property.call(instance)
            resultArray[i] = if (param.isEnum && value != null) (value as Enum<*>).name else value
        }
        return resultArray
    }

    fun toInsertParams(instances: Collection<T>, excludeId: Boolean): Array<Any?> {
        val params = if (!excludeId) constructorParameters else paramsWithoutId
        val resultArray = Array<Any?>(params.size * instances.size) { null }
        var counter = 0
        instances.forEach { instance: T ->
            params.forEach { param ->
                val value = param.property.call(instance)
                resultArray[counter++] = if (param.isEnum && value != null) (value as Enum<*>).name else value
            }
        }

        return resultArray
    }

    fun toUpdateParam(instance: T): Array<Any?> {
        val resultArray = createResultArray(constructorParameters.size)
        paramsWithoutId.forEachIndexed { i, param ->
            val value = param.property.call(instance)
            resultArray[i] = if (param.isEnum && value != null) (value as Enum<*>).name else value
        }
        val value = idParam.property.call(instance)
        resultArray[constructorParameters.size - 1] = if (idParam.isEnum && value != null) (value as Enum<*>).name else value
        return resultArray
    }

    fun toUpdateParams(instances: Collection<T>): Array<Any?> {
        val ids = instances.map {
            val value = idParam.property.call(it)
            if (idParam.isEnum && value != null) (value as Enum<*>).name else value
        }
        val resultArraySize = ((constructorParameters.size - 1) * instances.size * 2) + ids.size
        val resultArray = createResultArray(resultArraySize)
        var resultArrayCounter = 0
        paramsWithoutId.forEach { param ->
            instances.forEachIndexed { instanceIndex, instance ->
                resultArray[resultArrayCounter++] = ids[instanceIndex]
                val value = param.property.call(instance)
                resultArray[resultArrayCounter++] = if (param.isEnum && value != null) (value as Enum<*>).name else value
            }
        }

        for (id in ids) resultArray[resultArrayCounter++] = id
        return resultArray
    }

    fun toDeleteByIdParam(instance: T) = arrayOf(requireNotNull(idParam.property.call(instance)) { "@Id param: ${idParam.name} cannot be null!" })

    fun toDeleteByIdsParam(instances: Collection<T>) = instances.map {
        requireNotNull(idParam.property.call(it)) { "@Id param: ${idParam.name} cannot be null!" }
    }

    fun toConstructorParams(args: Array<Any?>): Array<Any?> {
        val resultArray = createResultArray(constructorParameters.size)
        constructorParameters.forEachIndexed { i, param ->
            val value = args[i]
            resultArray[i] = if (param.isEnum && value != null) (param.enumConstants!!.first { it.name == value }) else value
        }
        return resultArray
    }

    fun toConstructorParams(args: Array<Any?>, id: Any): Array<Any?> {
        val resultArray = createResultArray(constructorParameters.size)
        var foundId = false
        constructorParameters.forEachIndexed { i, param ->
            if (param.name == idParam.name) {
                val value = if (param.isEnum) (param.enumConstants!!.first { it.name == id }) else id
                resultArray[i] = value
                foundId = true
            } else {
                val argIndex = if (!foundId) i else i - 1
                val value = args[argIndex]
                resultArray[i] = if (param.isEnum && value != null) (param.enumConstants!!.first { it.name == value }) else value
            }
        }
        return resultArray
    }

    private fun createResultArray(size: Int): Array<Any?> = Array(size) { null }
}
