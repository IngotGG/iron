package gg.ingot.iron.transformer

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.repository.ModelRepository
import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.representation.EntityModel
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

/**
 * Transforms a model to an entity representation which holds information about the class model, allowing
 * for the model to be easily built. This class interfaces with reflection to build the entity.
 * @author Santio
 * @since 1.0
 */
internal object ModelTransformer {

    fun transform(clazz: KClass<*>): EntityModel {
        return ModelRepository.models.getOrPut(clazz) {
            val fields = mutableListOf<EntityField>()

            for (field in clazz.declaredMemberProperties) {
                val annotation = field.annotations.find { it is Column } as Column?

                if (annotation != null && annotation.ignore || field.javaField?.isSynthetic == true) {
                    continue
                }

                fields.add(EntityField(
                    field,
                    field.javaField ?: throw IllegalStateException("Field ${field.name} has no backing field."),
                    annotation?.name ?: field.name,
                    field.returnType.isMarkedNullable
                ))
            }

            EntityModel(clazz, fields)
        }
    }

}