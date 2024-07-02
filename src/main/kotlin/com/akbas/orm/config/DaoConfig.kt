package com.akbas.orm.config

import com.akbas.orm.entities.EntityDao
import com.akbas.orm.pagination.PaginationDao
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DaoConfig {

    @Bean
    fun entityDao(dataSource: DataSource): EntityDao {
        return EntityDao.create(dataSource, false)
    }

    @Bean
    fun paginationDao(entityDao: EntityDao) = PaginationDao(entityDao)
}