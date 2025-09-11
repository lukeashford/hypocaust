package com.example.the_machine.operator.result

/**
 * Base class for all result types in the operator system. Provides common success/failure
 * indication, messaging, and factory methods that can be utilized by concrete result types.
 */
abstract class BaseResult(
  /**
   * Whether the operation succeeded.
   */
  val ok: Boolean,
  /**
   * Human-readable message describing the result.
   */
  val message: String
) {

  /**
   * Protected helper for creating successful results. Enforces ok = true for all success cases.
   */
  protected fun <T : BaseResult> createSuccess(
    message: String,
    factory: ResultFactory<T>
  ): T = factory.create(true, message)

  /**
   * Protected helper for creating successful results with default message.
   */
  protected fun <T : BaseResult> createSuccess(factory: ResultFactory<T>): T =
    factory.create(true, "Operation completed successfully")

  /**
   * Protected helper for creating failed results.
   */
  protected fun <T : BaseResult> createFailure(
    message: String,
    factory: ResultFactory<T>
  ): T = factory.create(false, message)

  protected fun interface ResultFactory<T : BaseResult> {

    fun create(ok: Boolean, message: String): T
  }

  override fun toString(): String =
    "BaseResult{ok=$ok, message='$message'}"
}