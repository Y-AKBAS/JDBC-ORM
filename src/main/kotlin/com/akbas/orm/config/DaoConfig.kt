package com.akbas.orm.config

import com.akbas.orm.entities.EntityDao
import com.akbas.orm.pagination.PaginationDao
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.JdbcClientAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource


@AutoConfiguration(after = [JdbcClientAutoConfiguration::class])
@Configuration
@ConditionalOnBean(DataSource::class)
@ComponentScan(basePackages = ["com.akbas.orm"])
class DaoConfig {

    @Bean
    @ConditionalOnMissingBean
    fun entityDao(dataSource: DataSource): EntityDao {
        return EntityDao.create(dataSource, false)
    }

    @Bean
    @ConditionalOnMissingBean
    fun paginationDao(entityDao: EntityDao) = PaginationDao(entityDao)
}
