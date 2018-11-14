## Messages used in the Dough Preparation Stage

#### Kneading Request
- productType [String]
- kneadingTime [float]
- guid [Array Strings]

#### Kneading Notification
- productType [String]
- guid [String]

#### Preparation Request
- productType [String]
- productQuantity [Array int]
- restingTime [float]
- itemPreparationTime [float]
- guid [Array Strings]

#### Preparation Notification
- productType [String]
- guid [String]

#### Proofing Request
- productType [String]
- proofingTime [float]
- guid [Array Strings]

#### Dough Notification Message
- guid
