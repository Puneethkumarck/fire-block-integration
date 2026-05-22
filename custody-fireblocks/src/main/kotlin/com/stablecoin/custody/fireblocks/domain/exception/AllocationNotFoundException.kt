package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class AllocationNotFoundException(
    allocationId: String,
) : CustodyException(ErrorCode.ALLOCATION_NOT_FOUND, "Allocation not found: $allocationId")
