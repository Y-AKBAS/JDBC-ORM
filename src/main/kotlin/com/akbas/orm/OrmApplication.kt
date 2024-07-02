package com.akbas.orm

import com.akbas.orm.config.DBConnectionDetails
import com.akbas.orm.entities.EntityDao
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Component
import java.sql.Timestamp

@SpringBootApplication
class OrmApplication

fun main(args: Array<String>) {
    runApplication<OrmApplication>(*args)
}

@Table(CompositionEntity.TABLE_NAME)
class CompositionEntity(
	@Id var id: String?,
	var uid: Long?,
	var creator: String,
	var partnerId: Long,
	var mainDesignId: Long?,
	var mainDesignLookupId: String?,
	var designs: String?,
	var properties: String? = null,

	var assortmentRule: String? = null,
	var templateProducts: String?,
	var migratedSellables: String?,

	var metadata: String,
	var d2c: String?,
	var shops: String?,
	var publish: Boolean,
	var topics: String?,

	var dateCreated: Timestamp,
	var dateModified: Timestamp,
	var dateTouched: Timestamp,
	var accreditationTouchDate: Timestamp
) {

	companion object {
		const val TABLE_NAME = "compositions"
	}
}

@Component
class TestBadSql {

    @EventListener(ApplicationReadyEvent::class)
    fun test() {
        val localPubcDbProperties = DBConnectionDetails(
            url = "jdbc:mysql://localhost:3306/publishing_core",
            driverClassName = "com.mysql.cj.jdbc.Driver",
            userName = "root",
            password = "spreadshirt_local"
        )
        val entityDao = EntityDao.create(localPubcDbProperties, readOnly = false)
		var entities = entityDao.findAll<CompositionEntity>()
		println(entities.size)
		entityDao.jdbcClient.sql("DROP TABLE compositions")
		entities = entityDao.findAll<CompositionEntity>()
		println(entities.size)
		println()
    }
}
