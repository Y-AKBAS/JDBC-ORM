package com.akbas.orm.support

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Projection(val ofClass: KClass<*>)
