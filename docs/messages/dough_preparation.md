## Messages used in the Dough Preparation Stage

#### Kneading Request
- productType [String]
- kneadingTime [float]
- guids [Array Strings]

#### Kneading Notification
- productType [String]
- guid [String]

#### Preparation Request
- productType [String]
- productQuantities [Array int]
- steps [Array JSON Objects]
- guids [Array Strings]

Step
- action [String]
- duration [float]

#### Preparation Notification
- productType [String]
- guid [String]

#### Proofing Request
- productType [String]
- proofingTime [float]
- guids [Array Strings]

#### Dough Notification Message
- guid [String]
