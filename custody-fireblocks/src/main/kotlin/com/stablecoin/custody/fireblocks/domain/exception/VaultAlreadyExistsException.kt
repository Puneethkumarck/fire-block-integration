package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class VaultAlreadyExistsException(
    customerRefId: String,
) : CustodyException(ErrorCode.VAULT_ALREADY_EXISTS, "Vault already exists for customerRefId: $customerRefId")
