/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.UByte

/**
 * Minimal s2n-tls C bindings for the Native TLS client engine (design Phase 3). Opaque structs are
 * represented as `Ptr[Byte]`; `ssize_t`/`size_t` are bound as `CLong` (LP64 targets). IO is driven
 * through the custom recv/send callbacks so the engine composes with the blocking-socket baseline.
 */
@link("s2n")
@extern
private[net] object S2n:

  /** `int s2n_init(void)` — must be called once before any other s2n API. */
  def s2n_init(): CInt = extern

  /** `struct s2n_config *s2n_config_new(void)` — new config preloaded with the default trust store. */
  def s2n_config_new(): Ptr[Byte] = extern

  /** `int s2n_config_free(struct s2n_config *config)` */
  def s2n_config_free(config: Ptr[Byte]): CInt = extern

  /** `int s2n_config_wipe_trust_store(struct s2n_config *config)` */
  def s2n_config_wipe_trust_store(config: Ptr[Byte]): CInt = extern

  /** `int s2n_config_add_pem_to_trust_store(struct s2n_config *config, const char *pem)` */
  def s2n_config_add_pem_to_trust_store(config: Ptr[Byte], pem: CString): CInt = extern

  /** `int s2n_config_disable_x509_verification(struct s2n_config *config)` */
  def s2n_config_disable_x509_verification(config: Ptr[Byte]): CInt = extern

  /** `int s2n_config_set_verify_host_callback(config, s2n_verify_host_fn, void *data)` — one name per call. */
  def s2n_config_set_verify_host_callback(
    config: Ptr[Byte],
    cb:     CFuncPtr3[CString, CLong, Ptr[Byte], UByte],
    data:   Ptr[Byte]
  ): CInt = extern

  /** `struct s2n_connection *s2n_connection_new(s2n_mode mode)` — `S2N_CLIENT = 1`. */
  def s2n_connection_new(mode: CInt): Ptr[Byte] = extern

  /** `int s2n_connection_free(struct s2n_connection *conn)` */
  def s2n_connection_free(conn: Ptr[Byte]): CInt = extern

  /** `int s2n_connection_set_config(struct s2n_connection *conn, struct s2n_config *config)` */
  def s2n_connection_set_config(conn: Ptr[Byte], config: Ptr[Byte]): CInt = extern

  /** `int s2n_connection_set_blinding(conn, s2n_blinding)` — `S2N_SELF_SERVICE_BLINDING = 1`. */
  def s2n_connection_set_blinding(conn: Ptr[Byte], blinding: CInt): CInt = extern

  /** `int s2n_set_server_name(struct s2n_connection *conn, const char *server_name)` — SNI. */
  def s2n_set_server_name(conn: Ptr[Byte], name: CString): CInt = extern

  /** `int s2n_connection_set_recv_cb(conn, int (*)(void *ctx, uint8_t *buf, uint32_t len))` */
  def s2n_connection_set_recv_cb(conn: Ptr[Byte], cb: CFuncPtr3[Ptr[Byte], Ptr[Byte], CUnsignedInt, CInt]): CInt =
    extern

  /** `int s2n_connection_set_send_cb(conn, int (*)(void *ctx, const uint8_t *buf, uint32_t len))` */
  def s2n_connection_set_send_cb(conn: Ptr[Byte], cb: CFuncPtr3[Ptr[Byte], Ptr[Byte], CUnsignedInt, CInt]): CInt =
    extern

  /** `int s2n_connection_set_recv_ctx(struct s2n_connection *conn, void *ctx)` */
  def s2n_connection_set_recv_ctx(conn: Ptr[Byte], ctx: Ptr[Byte]): CInt = extern

  /** `int s2n_connection_set_send_ctx(struct s2n_connection *conn, void *ctx)` */
  def s2n_connection_set_send_ctx(conn: Ptr[Byte], ctx: Ptr[Byte]): CInt = extern

  /** `int s2n_negotiate(struct s2n_connection *conn, s2n_blocked_status *blocked)` */
  def s2n_negotiate(conn: Ptr[Byte], blocked: Ptr[CInt]): CInt = extern

  /** `ssize_t s2n_send(conn, const void *buf, ssize_t size, s2n_blocked_status *blocked)` */
  def s2n_send(conn: Ptr[Byte], buf: Ptr[Byte], size: CLong, blocked: Ptr[CInt]): CLong = extern

  /** `ssize_t s2n_recv(conn, void *buf, ssize_t size, s2n_blocked_status *blocked)` */
  def s2n_recv(conn: Ptr[Byte], buf: Ptr[Byte], size: CLong, blocked: Ptr[CInt]): CLong = extern

  /** `int s2n_shutdown_send(struct s2n_connection *conn, s2n_blocked_status *blocked)` — half-close. */
  def s2n_shutdown_send(conn: Ptr[Byte], blocked: Ptr[CInt]): CInt = extern
