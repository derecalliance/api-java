- Message names may be simplified:


    PairHelperRequestMessage - > PairRequest
    PairHelperResponseMessage -> PairResponse
    UnpairHelperRequestMessage -> UnpairRequest
    UnpairHelperResponseMessage -> UnpairResponse
    etc.

- Field numbers must be positive integers i.e. not 0

- `boolean` should be `bool`

- in responses, "complied" would be better rendered as status (an enum) and 
provision made for a memo field

- Comments in message headers need updating.

- it would be preferable for message batch to be replaced by allowing 
repeated message bodies in DeRecMessage, since they will all refer to 
the same secret anyway

- worth considering sticking to protobuf convention of underscore 
separated field names

- need to review StoreShareRequestMessage
