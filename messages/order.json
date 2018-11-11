{
     "order": [
       {
         "customer_id": 001,
         "order_date": "00.00",
         "delivery_date": "00.00",
         "list_products": [1, 2, 3]
       }
       ]
 }

 Schema:

 {
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "order": {
      "type": "array",
      "items": [
        {
          "type": "object",
          "properties": {
            "customer_id": {
              "type": "integer"
            },
            "order_date": {
              "type": "string"
            },
            "delivery_date": {
              "type": "string"
            },
            "list_products": {
              "type": "array",
              "items": [
                {
                  "type": "integer"
                },
                {
                  "type": "integer"
                },
                {
                  "type": "integer"
                }
              ]
            }
          },
          "required": [
            "customer_id",
            "order_date",
            "delivery_date",
            "list_products"
          ]
        }
      ]
    }
  },
  "required": [
    "order"
  ]
}
