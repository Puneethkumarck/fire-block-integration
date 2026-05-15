package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class VaultNotFoundException(
    customerRefId: String,
) : CustodyException(ErrorCode.VAULT_NOT_FOUND, "Vault not found for customerRefId: $customerRefId")
