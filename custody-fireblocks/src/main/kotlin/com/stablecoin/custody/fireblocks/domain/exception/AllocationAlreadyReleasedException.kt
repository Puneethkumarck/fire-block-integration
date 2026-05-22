package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class AllocationAlreadyReleasedException(
    allocationId: String,
) : CustodyException(ErrorCode.ALLOCATION_ALREADY_RELEASED, "Allocation already released: $allocationId")
