package ru.pixnews.wasm.sqlite3.chicory.bindings

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite3.chicory.ext.WASM_ADDR_SIZE
import ru.pixnews.wasm.sqlite3.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.readAddr
import ru.pixnews.wasm.sqlite3.chicory.ext.readNullTerminatedString
import ru.pixnews.wasm.sqlite3.chicory.wasi.preview1.type.Errno

class SqliteBindings(
    public val memory: Memory,
    private val runtimeInstance: Instance,
) {
    val _initialize = runtimeInstance.export("_initialize") // 34
    // val __errno_location = runtimeInstance.export("__errno_location") // 2644
    // val __wasm_call_ctors = runtimeInstance.export("__wasm_call_ctors") // 34
    val __indirect_function_table = runtimeInstance.export("__indirect_function_table") // 0

    val sqlite3_status64 = runtimeInstance.export("sqlite3_status64") // 35
    val sqlite3_status = runtimeInstance.export("sqlite3_status") // 38
    val sqlite3_db_status = runtimeInstance.export("sqlite3_db_status") // 39
    val sqlite3_msize = runtimeInstance.export("sqlite3_msize") // 48
    val sqlite3_vfs_find = runtimeInstance.export("sqlite3_vfs_find") // 58
    val sqlite3_initialize = runtimeInstance.export("sqlite3_initialize") // 59
    val sqlite3_vfs_register = runtimeInstance.export("sqlite3_vfs_register") // 66
    val sqlite3_vfs_unregister = runtimeInstance.export("sqlite3_vfs_unregister") // 69

    val sqlite3_value_text = runtimeInstance.export("sqlite3_value_text") // 95
    val sqlite3_randomness = runtimeInstance.export("sqlite3_randomness") // 107
    val sqlite3_stricmp = runtimeInstance.export("sqlite3_stricmp") // 108
    val sqlite3_strnicmp = runtimeInstance.export("sqlite3_strnicmp") // 110
    val sqlite3_uri_parameter = runtimeInstance.export("sqlite3_uri_parameter") // 114
    val sqlite3_uri_boolean = runtimeInstance.export("sqlite3_uri_boolean") // 116
    val sqlite3_serialize = runtimeInstance.export("sqlite3_serialize") // 132
    val sqlite3_prepare_v2 = runtimeInstance.export("sqlite3_prepare_v2") // 135
    val sqlite3_step = runtimeInstance.export("sqlite3_step") // 136
    val sqlite3_column_int64 = runtimeInstance.export("sqlite3_column_int64") // 137
    val sqlite3_column_int = runtimeInstance.export("sqlite3_column_int") // 138
    val sqlite3_finalize = runtimeInstance.export("sqlite3_finalize") // 140
    val sqlite3_file_control = runtimeInstance.export("sqlite3_file_control") // 141
    val sqlite3_reset = runtimeInstance.export("sqlite3_reset") // 146
    val sqlite3_deserialize = runtimeInstance.export("sqlite3_deserialize") // 165
    val sqlite3_clear_bindings = runtimeInstance.export("sqlite3_clear_bindings") // 240
    val sqlite3_value_blob = runtimeInstance.export("sqlite3_value_blob") // 243
    val sqlite3_value_bytes = runtimeInstance.export("sqlite3_value_bytes") // 247
    val sqlite3_value_double = runtimeInstance.export("sqlite3_value_double") // 251
    val sqlite3_value_int = runtimeInstance.export("sqlite3_value_int") // 253
    val sqlite3_value_int64 = runtimeInstance.export("sqlite3_value_int64") // 255
    val sqlite3_value_subtype = runtimeInstance.export("sqlite3_value_subtype") // 256
    val sqlite3_value_pointer = runtimeInstance.export("sqlite3_value_pointer") // 257
    val sqlite3_value_type = runtimeInstance.export("sqlite3_value_type") // 259
    val sqlite3_value_nochange = runtimeInstance.export("sqlite3_value_nochange") // 260
    val sqlite3_value_frombind = runtimeInstance.export("sqlite3_value_frombind") // 261
    val sqlite3_value_dup = runtimeInstance.export("sqlite3_value_dup") // 262
    val sqlite3_value_free = runtimeInstance.export("sqlite3_value_free") // 265
    val sqlite3_result_blob = runtimeInstance.export("sqlite3_result_blob") // 266
    val sqlite3_result_error_toobig = runtimeInstance.export("sqlite3_result_error_toobig") // 269
    val sqlite3_result_error_nomem = runtimeInstance.export("sqlite3_result_error_nomem") // 270
    val sqlite3_result_double = runtimeInstance.export("sqlite3_result_double") // 273
    val sqlite3_result_error = runtimeInstance.export("sqlite3_result_error") // 276
    val sqlite3_result_int = runtimeInstance.export("sqlite3_result_int") // 279
    val sqlite3_result_int64 = runtimeInstance.export("sqlite3_result_int64") // 281
    val sqlite3_result_null = runtimeInstance.export("sqlite3_result_null") // 282
    val sqlite3_result_pointer = runtimeInstance.export("sqlite3_result_pointer") // 284
    val sqlite3_result_subtype = runtimeInstance.export("sqlite3_result_subtype") // 287
    val sqlite3_result_text = runtimeInstance.export("sqlite3_result_text") // 288
    val sqlite3_result_zeroblob = runtimeInstance.export("sqlite3_result_zeroblob") // 294
    val sqlite3_result_zeroblob64 = runtimeInstance.export("sqlite3_result_zeroblob64") // 295
    val sqlite3_result_error_code = runtimeInstance.export("sqlite3_result_error_code") // 297
    val sqlite3_user_data = runtimeInstance.export("sqlite3_user_data") // 302
    val sqlite3_context_db_handle = runtimeInstance.export("sqlite3_context_db_handle") // 303
    val sqlite3_vtab_nochange = runtimeInstance.export("sqlite3_vtab_nochange") // 304
    val sqlite3_vtab_in_first = runtimeInstance.export("sqlite3_vtab_in_first") // 305
    val sqlite3_vtab_in_next = runtimeInstance.export("sqlite3_vtab_in_next") // 314
    val sqlite3_aggregate_context = runtimeInstance.export("sqlite3_aggregate_context") // 315
    val sqlite3_get_auxdata = runtimeInstance.export("sqlite3_get_auxdata") // 317
    val sqlite3_set_auxdata = runtimeInstance.export("sqlite3_set_auxdata") // 318
    val sqlite3_column_count = runtimeInstance.export("sqlite3_column_count") // 320
    val sqlite3_data_count = runtimeInstance.export("sqlite3_data_count") // 321
    val sqlite3_column_blob = runtimeInstance.export("sqlite3_column_blob") // 322
    val sqlite3_column_bytes = runtimeInstance.export("sqlite3_column_bytes") // 323
    val sqlite3_column_double = runtimeInstance.export("sqlite3_column_double") // 324
    val sqlite3_column_text = runtimeInstance.export("sqlite3_column_text") // 325
    val sqlite3_column_value = runtimeInstance.export("sqlite3_column_value") // 326
    val sqlite3_column_type = runtimeInstance.export("sqlite3_column_type") // 327
    val sqlite3_column_name = runtimeInstance.export("sqlite3_column_name") // 328
    val sqlite3_bind_blob = runtimeInstance.export("sqlite3_bind_blob") // 330
    val sqlite3_bind_double = runtimeInstance.export("sqlite3_bind_double") // 333
    val sqlite3_bind_int = runtimeInstance.export("sqlite3_bind_int") // 334
    val sqlite3_bind_int64 = runtimeInstance.export("sqlite3_bind_int64") // 335
    val sqlite3_bind_null = runtimeInstance.export("sqlite3_bind_null") // 336
    val sqlite3_bind_pointer = runtimeInstance.export("sqlite3_bind_pointer") // 337
    val sqlite3_bind_text = runtimeInstance.export("sqlite3_bind_text") // 338
    val sqlite3_bind_parameter_count = runtimeInstance.export("sqlite3_bind_parameter_count") // 341
    val sqlite3_bind_parameter_index = runtimeInstance.export("sqlite3_bind_parameter_index") // 343
    val sqlite3_db_handle = runtimeInstance.export("sqlite3_db_handle") // 346
    val sqlite3_stmt_readonly = runtimeInstance.export("sqlite3_stmt_readonly") // 347
    val sqlite3_stmt_isexplain = runtimeInstance.export("sqlite3_stmt_isexplain") // 348
    val sqlite3_stmt_status = runtimeInstance.export("sqlite3_stmt_status") // 350
    val sqlite3_sql = runtimeInstance.export("sqlite3_sql") // 351
    val sqlite3_expanded_sql = runtimeInstance.export("sqlite3_expanded_sql") // 352
    val sqlite3_preupdate_old = runtimeInstance.export("sqlite3_preupdate_old") // 355
    val sqlite3_preupdate_count = runtimeInstance.export("sqlite3_preupdate_count") // 365
    val sqlite3_preupdate_depth = runtimeInstance.export("sqlite3_preupdate_depth") // 366
    val sqlite3_preupdate_blobwrite = runtimeInstance.export("sqlite3_preupdate_blobwrite") // 367
    val sqlite3_preupdate_new = runtimeInstance.export("sqlite3_preupdate_new") // 368
    val sqlite3_value_numeric_type = runtimeInstance.export("sqlite3_value_numeric_type") // 369
    val sqlite3_errmsg = runtimeInstance.export("sqlite3_errmsg") // 396
    val sqlite3_set_authorizer = runtimeInstance.export("sqlite3_set_authorizer") // 409
    val sqlite3_strglob = runtimeInstance.export("sqlite3_strglob") // 411
    val sqlite3_strlike = runtimeInstance.export("sqlite3_strlike") // 414
    val sqlite3_exec = runtimeInstance.export("sqlite3_exec") // 415
    val sqlite3_auto_extension = runtimeInstance.export("sqlite3_auto_extension") // 416
    val sqlite3_cancel_auto_extension = runtimeInstance.export("sqlite3_cancel_auto_extension") // 417
    val sqlite3_reset_auto_extension = runtimeInstance.export("sqlite3_reset_auto_extension") // 418
    val sqlite3_prepare_v3 = runtimeInstance.export("sqlite3_prepare_v3") // 422
    val sqlite3_create_module = runtimeInstance.export("sqlite3_create_module") // 423
    val sqlite3_create_module_v2 = runtimeInstance.export("sqlite3_create_module_v2") // 425
    val sqlite3_drop_modules = runtimeInstance.export("sqlite3_drop_modules") // 426
    val sqlite3_declare_vtab = runtimeInstance.export("sqlite3_declare_vtab") // 427
    val sqlite3_vtab_on_conflict = runtimeInstance.export("sqlite3_vtab_on_conflict") // 436
    val sqlite3_vtab_collation = runtimeInstance.export("sqlite3_vtab_collation") // 438
    val sqlite3_vtab_in = runtimeInstance.export("sqlite3_vtab_in") // 441
    val sqlite3_vtab_rhs_value = runtimeInstance.export("sqlite3_vtab_rhs_value") // 442
    val sqlite3_vtab_distinct = runtimeInstance.export("sqlite3_vtab_distinct") // 445
    val sqlite3_keyword_name = runtimeInstance.export("sqlite3_keyword_name") // 446
    val sqlite3_keyword_count = runtimeInstance.export("sqlite3_keyword_count") // 447
    val sqlite3_keyword_check = runtimeInstance.export("sqlite3_keyword_check") // 448
    val sqlite3_complete = runtimeInstance.export("sqlite3_complete") // 451
    val sqlite3_libversion = runtimeInstance.export("sqlite3_libversion") // 452
    val sqlite3_libversion_number = runtimeInstance.export("sqlite3_libversion_number") // 453
    val sqlite3_shutdown = runtimeInstance.export("sqlite3_shutdown") // 454
    val sqlite3_last_insert_rowid = runtimeInstance.export("sqlite3_last_insert_rowid") // 460
    val sqlite3_set_last_insert_rowid = runtimeInstance.export("sqlite3_set_last_insert_rowid") // 461
    val sqlite3_changes64 = runtimeInstance.export("sqlite3_changes64") // 462
    val sqlite3_changes = runtimeInstance.export("sqlite3_changes") // 463
    val sqlite3_total_changes64 = runtimeInstance.export("sqlite3_total_changes64") // 464
    val sqlite3_total_changes = runtimeInstance.export("sqlite3_total_changes") // 465
    val sqlite3_txn_state = runtimeInstance.export("sqlite3_txn_state") // 466
    private val sqlite3_close_v2 = runtimeInstance.export("sqlite3_close_v2") // 471
    val sqlite3_busy_handler = runtimeInstance.export("sqlite3_busy_handler") // 472
    val sqlite3_progress_handler = runtimeInstance.export("sqlite3_progress_handler") // 473
    val sqlite3_busy_timeout = runtimeInstance.export("sqlite3_busy_timeout") // 474
    val sqlite3_create_function = runtimeInstance.export("sqlite3_create_function") // 476
    val sqlite3_create_function_v2 = runtimeInstance.export("sqlite3_create_function_v2") // 479
    val sqlite3_create_window_function = runtimeInstance.export("sqlite3_create_window_function") // 480
    val sqlite3_overload_function = runtimeInstance.export("sqlite3_overload_function") // 481
    val sqlite3_trace_v2 = runtimeInstance.export("sqlite3_trace_v2") // 487
    val sqlite3_commit_hook = runtimeInstance.export("sqlite3_commit_hook") // 488
    val sqlite3_update_hook = runtimeInstance.export("sqlite3_update_hook") // 489
    val sqlite3_rollback_hook = runtimeInstance.export("sqlite3_rollback_hook") // 490
    val sqlite3_preupdate_hook = runtimeInstance.export("sqlite3_preupdate_hook") // 491
    val sqlite3_error_offset = runtimeInstance.export("sqlite3_error_offset") // 499
    val sqlite3_errcode = runtimeInstance.export("sqlite3_errcode") // 500
    val sqlite3_extended_errcode = runtimeInstance.export("sqlite3_extended_errcode") // 501
    val sqlite3_errstr = runtimeInstance.export("sqlite3_errstr") // 502
    val sqlite3_limit = runtimeInstance.export("sqlite3_limit") // 503
    private val sqlite3_open = runtimeInstance.export("sqlite3_open") // 504
    val sqlite3_open_v2 = runtimeInstance.export("sqlite3_open_v2") // 515
    val sqlite3_create_collation = runtimeInstance.export("sqlite3_create_collation") // 516
    val sqlite3_create_collation_v2 = runtimeInstance.export("sqlite3_create_collation_v2") // 517
    val sqlite3_collation_needed = runtimeInstance.export("sqlite3_collation_needed") // 519
    val sqlite3_get_autocommit = runtimeInstance.export("sqlite3_get_autocommit") // 520
    val sqlite3_table_column_metadata = runtimeInstance.export("sqlite3_table_column_metadata") // 521
    val sqlite3_extended_result_codes = runtimeInstance.export("sqlite3_extended_result_codes") // 527
    val sqlite3_uri_key = runtimeInstance.export("sqlite3_uri_key") // 541
    val sqlite3_uri_int64 = runtimeInstance.export("sqlite3_uri_int64") // 544
    val sqlite3_db_name = runtimeInstance.export("sqlite3_db_name") // 546
    val sqlite3_db_filename = runtimeInstance.export("sqlite3_db_filename") // 547
    val sqlite3_compileoption_used = runtimeInstance.export("sqlite3_compileoption_used") // 549
    val sqlite3_compileoption_get = runtimeInstance.export("sqlite3_compileoption_get") // 550

    val sqlite3session_diff = runtimeInstance.export("sqlite3session_diff") // 551
    val sqlite3session_attach = runtimeInstance.export("sqlite3session_attach") // 566
    val sqlite3session_create = runtimeInstance.export("sqlite3session_create") // 570
    val sqlite3session_delete = runtimeInstance.export("sqlite3session_delete") // 572
    val sqlite3session_table_filter = runtimeInstance.export("sqlite3session_table_filter") // 574
    val sqlite3session_changeset = runtimeInstance.export("sqlite3session_changeset") // 575
    val sqlite3session_changeset_strm = runtimeInstance.export("sqlite3session_changeset_strm") // 586
    val sqlite3session_patchset_strm = runtimeInstance.export("sqlite3session_patchset_strm") // 587
    val sqlite3session_patchset = runtimeInstance.export("sqlite3session_patchset") // 588
    val sqlite3session_enable = runtimeInstance.export("sqlite3session_enable") // 589
    val sqlite3session_indirect = runtimeInstance.export("sqlite3session_indirect") // 590
    val sqlite3session_isempty = runtimeInstance.export("sqlite3session_isempty") // 591
    val sqlite3session_memory_used = runtimeInstance.export("sqlite3session_memory_used") // 592
    val sqlite3session_object_config = runtimeInstance.export("sqlite3session_object_config") // 593
    val sqlite3session_changeset_size = runtimeInstance.export("sqlite3session_changeset_size") // 594

    val sqlite3changeset_start = runtimeInstance.export("sqlite3changeset_start") // 595
    val sqlite3changeset_start_v2 = runtimeInstance.export("sqlite3changeset_start_v2") // 597
    val sqlite3changeset_start_strm = runtimeInstance.export("sqlite3changeset_start_strm") // 598
    val sqlite3changeset_start_v2_strm = runtimeInstance.export("sqlite3changeset_start_v2_strm") // 599
    val sqlite3changeset_next = runtimeInstance.export("sqlite3changeset_next") // 600
    val sqlite3changeset_op = runtimeInstance.export("sqlite3changeset_op") // 608
    val sqlite3changeset_pk = runtimeInstance.export("sqlite3changeset_pk") // 609
    val sqlite3changeset_old = runtimeInstance.export("sqlite3changeset_old") // 610
    val sqlite3changeset_new = runtimeInstance.export("sqlite3changeset_new") // 611
    val sqlite3changeset_conflict = runtimeInstance.export("sqlite3changeset_conflict") // 612
    val sqlite3changeset_fk_conflicts = runtimeInstance.export("sqlite3changeset_fk_conflicts") // 613
    val sqlite3changeset_finalize = runtimeInstance.export("sqlite3changeset_finalize") // 614
    val sqlite3changeset_invert = runtimeInstance.export("sqlite3changeset_invert") // 615
    val sqlite3changeset_invert_strm = runtimeInstance.export("sqlite3changeset_invert_strm") // 618
    val sqlite3changeset_apply_v2 = runtimeInstance.export("sqlite3changeset_apply_v2") // 619
    val sqlite3changeset_apply = runtimeInstance.export("sqlite3changeset_apply") // 629
    val sqlite3changeset_apply_v2_strm = runtimeInstance.export("sqlite3changeset_apply_v2_strm") // 630
    val sqlite3changeset_apply_strm = runtimeInstance.export("sqlite3changeset_apply_strm") // 631
    val sqlite3changegroup_new = runtimeInstance.export("sqlite3changegroup_new") // 632
    val sqlite3changegroup_add = runtimeInstance.export("sqlite3changegroup_add") // 633
    val sqlite3changegroup_output = runtimeInstance.export("sqlite3changegroup_output") // 645
    val sqlite3changegroup_add_strm = runtimeInstance.export("sqlite3changegroup_add_strm") // 647
    val sqlite3changegroup_output_strm = runtimeInstance.export("sqlite3changegroup_output_strm") // 648
    val sqlite3changegroup_delete = runtimeInstance.export("sqlite3changegroup_delete") // 649
    val sqlite3changeset_concat = runtimeInstance.export("sqlite3changeset_concat") // 650
    val sqlite3changeset_concat_strm = runtimeInstance.export("sqlite3changeset_concat_strm") // 651
    val sqlite3session_config = runtimeInstance.export("sqlite3session_config") // 652
    val sqlite3_sourceid = runtimeInstance.export("sqlite3_sourceid") // 653

    val sqlite3_wasm_pstack_ptr = runtimeInstance.export("sqlite3__wasm_pstack_ptr") // 654
    val sqlite3_wasm_pstack_restore = runtimeInstance.export("sqlite3__wasm_pstack_restore") // 655
    val sqlite3_wasm_pstack_alloc = runtimeInstance.export("sqlite3__wasm_pstack_alloc") // 656
    val sqlite3_wasm_pstack_remaining = runtimeInstance.export("sqlite3__wasm_pstack_remaining") // 657
    val sqlite3_wasm_pstack_quota = runtimeInstance.export("sqlite3__wasm_pstack_quota") // 658
    val sqlite3_wasm_db_error = runtimeInstance.export("sqlite3__wasm_db_error") // 659
    val sqlite3_wasm_test_struct = runtimeInstance.export("sqlite3__wasm_test_struct") // 660
    val sqlite3_wasm_enum_json = runtimeInstance.export("sqlite3__wasm_enum_json") // 661
    val sqlite3_wasm_vfs_unlink = runtimeInstance.export("sqlite3__wasm_vfs_unlink") // 662
    val sqlite3_wasm_db_vfs = runtimeInstance.export("sqlite3__wasm_db_vfs") // 663
    val sqlite3_wasm_db_reset = runtimeInstance.export("sqlite3__wasm_db_reset") // 664
    val sqlite3_wasm_db_export_chunked = runtimeInstance.export("sqlite3__wasm_db_export_chunked") // 665
    val sqlite3_wasm_db_serialize = runtimeInstance.export("sqlite3__wasm_db_serialize") // 666
    val sqlite3_wasm_vfs_create_file = runtimeInstance.export("sqlite3__wasm_vfs_create_file") // 667
    val sqlite3_wasm_posix_create_file = runtimeInstance.export("sqlite3__wasm_posix_create_file") // 669
    val sqlite3_wasm_kvvfsMakeKeyOnPstack = runtimeInstance.export("sqlite3__wasm_kvvfsMakeKeyOnPstack") // 670
    val sqlite3_wasm_kvvfs_methods = runtimeInstance.export("sqlite3__wasm_kvvfs_methods") // 672
    val sqlite3_wasm_vtab_config = runtimeInstance.export("sqlite3__wasm_vtab_config") // 673
    val sqlite3_wasm_db_config_ip = runtimeInstance.export("sqlite3__wasm_db_config_ip") // 674
    val sqlite3_wasm_db_config_pii = runtimeInstance.export("sqlite3__wasm_db_config_pii") // 675
    val sqlite3_wasm_db_config_s = runtimeInstance.export("sqlite3__wasm_db_config_s") // 676
    val sqlite3_wasm_config_i = runtimeInstance.export("sqlite3__wasm_config_i") // 677
    val sqlite3_wasm_config_ii = runtimeInstance.export("sqlite3__wasm_config_ii") // 678
    val sqlite3_wasm_config_j = runtimeInstance.export("sqlite3__wasm_config_j") // 679
    val sqlite3_wasm_init_wasmfs = runtimeInstance.export("sqlite3__wasm_init_wasmfs") // 680
    val sqlite3_wasm_test_intptr = runtimeInstance.export("sqlite3__wasm_test_intptr") // 681
    val sqlite3_wasm_test_voidptr = runtimeInstance.export("sqlite3__wasm_test_voidptr") // 682
    val sqlite3_wasm_test_int64_max = runtimeInstance.export("sqlite3__wasm_test_int64_max") // 683
    val sqlite3_wasm_test_int64_min = runtimeInstance.export("sqlite3__wasm_test_int64_min") // 684
    val sqlite3_wasm_test_int64_times2 = runtimeInstance.export("sqlite3__wasm_test_int64_times2") // 685
    val sqlite3_wasm_test_int64_minmax = runtimeInstance.export("sqlite3__wasm_test_int64_minmax") // 686
    val sqlite3_wasm_test_int64ptr = runtimeInstance.export("sqlite3__wasm_test_int64ptr") // 687
    val sqlite3_wasm_test_stack_overflow = runtimeInstance.export("sqlite3__wasm_test_stack_overflow") // 688
    val sqlite3_wasm_test_str_hello = runtimeInstance.export("sqlite3__wasm_test_str_hello") // 689
    val sqlite3_wasm_SQLTester_strglob = runtimeInstance.export("sqlite3__wasm_SQLTester_strglob") // 690

    public val dynamicMemory = SqliteDynamicMem(memory, runtimeInstance)

    val sqlite3Version: String
        get() {
            val resultPtr = sqlite3_libversion.apply()[0]
            return checkNotNull(memory.readNullTerminatedString(resultPtr))
        }

    val sqlite3SourceId: String
        get() {
            val resultPtr = sqlite3_sourceid.apply()[0].asInt()
            return checkNotNull(memory.readNullTerminatedString(resultPtr))
        }

    val sqlite3VersionNumber: Int
        get() = sqlite3_libversion_number.apply()[0].asInt()

    val sqlite3WasmEnumJson: String?
        get() {
            val resultPtr = sqlite3_wasm_enum_json.apply()[0]
            return memory.readNullTerminatedString(resultPtr)
        }


    fun sqlite3Open(
        filename: String,
    ): Value {
        var ppDb: Value? = null
        var pFileName: Value? = null
        var pDb: Value? = null
        try {
            ppDb = dynamicMemory.allocOrThrow(WASM_ADDR_SIZE)
            pFileName = dynamicMemory.allocNullTerminatedString(filename)

            val result = sqlite3_open.apply(pFileName, ppDb)

            pDb = memory.readAddr(ppDb.asWasmAddr())
            result.throwOnSqliteError("sqlite3_open() failed", pDb)

            return pDb
        } catch (e: Throwable) {
            pDb?.let { sqlite3Close(it) }
            throw e
        } finally {
            ppDb?.let { dynamicMemory.free(it) }
            pFileName?.let { dynamicMemory.free(it) }
        }
    }

    fun sqlite3Close(
        sqliteDb: Value
    ) {
        sqlite3_close_v2.apply(sqliteDb)
            .throwOnSqliteError("sqlite3_close_v2() failed", sqliteDb)
    }

    fun sqlite3ErrMsg(
        sqliteDb: Value
    ): String? {
        val p = sqlite3_errmsg.apply(sqliteDb)[0]
        return memory.readNullTerminatedString(p)
    }

    fun sqlite3ErrCode(
        sqliteDb: Value
    ): Int {
        return sqlite3_errcode.apply(sqliteDb)[0].asInt()
    }

    fun sqlite3ExtendedErrCode(
        sqliteDb: Value
    ): Int {
        return sqlite3_extended_errcode.apply(sqliteDb)[0].asInt()
    }

    private fun Array<Value>.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: Value? = null,
    ) {
        check(this.size == 1) { "Not an errno" }
        val errNo = this[0]
        if (errNo != Errno.SUCCESS.value) {
            val extendedErrCode: Int
            val errMsg: String
            if (sqliteDb != null) {
                extendedErrCode = sqlite3ExtendedErrCode(sqliteDb)
                errMsg = sqlite3ErrMsg(sqliteDb) ?: "null"
            } else {
                extendedErrCode = -1
                errMsg = ""
            }

            throw Sqlite3Error(errNo.asInt(), extendedErrCode, msgPrefix, errMsg)
        }
    }

    init {
        initSqlite()
    }

    // globalThis.sqlite3InitModule
    private fun initSqlite() {
        // __wasm_call_ctors.execute()
        _initialize.apply()
    }

}