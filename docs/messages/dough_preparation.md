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
- productQuantitys [Array int]
- restingTime [float]
- itemPreparationTime [float]
- guids [Array Strings]

#### Preparation Notification
- productType [String]
- guid [String]

#### Proofing Request
- productType [String]
- proofingTime [float]
- guids [Array Strings]

#### Dough Notification Message
- guid
