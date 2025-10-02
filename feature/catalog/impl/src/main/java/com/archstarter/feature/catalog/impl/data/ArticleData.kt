package com.archstarter.feature.catalog.impl.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "articles")
data class ArticleEntity(
  @PrimaryKey val id: Int,
  val title: String,
  val summary: String,
  val summaryLanguage: String?,
  val content: String,
  val sourceUrl: String,
  val originalWord: String,
  val translatedWord: String,
  val ipa: String?,
  val createdAt: Long
)

@Dao
interface ArticleDao {
  @Query("SELECT * FROM articles ORDER BY createdAt DESC")
  fun getArticles(): Flow<List<ArticleEntity>>

  @Query("SELECT * FROM articles WHERE id = :id")
  fun observeArticle(id: Int): Flow<ArticleEntity?>

  @Query("SELECT * FROM articles WHERE id = :id")
  suspend fun getArticle(id: Int): ArticleEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(article: ArticleEntity)
}

@Entity(tableName = "translations", primaryKeys = ["langPair", "normalizedText"])
data class TranslationEntity(
  val langPair: String,
  val normalizedText: String,
  val translation: String,
  val updatedAt: Long,
)

@Dao
interface TranslationDao {
  @Query(
    "SELECT * FROM translations WHERE langPair = :langPair AND normalizedText = :normalized LIMIT 1"
  )
  suspend fun translation(langPair: String, normalized: String): TranslationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entity: TranslationEntity)
}

@Database(entities = [ArticleEntity::class, TranslationEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
  abstract fun articleDao(): ArticleDao
  abstract fun translationDao(): TranslationDao
}
