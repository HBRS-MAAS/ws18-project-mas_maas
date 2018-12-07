# Messages used for the Baking Stage

## Dough Notification Messages

#### Content

```
{
    "productType": String,
    "guids": Vector<String>,
    "productQuantities": Vector <int>
}
```

## Baking Request

#### Content

```
{
    "bakingTemp": int,
    "bakingTime": float,
    "productType": String,
    "guids": Vector<String>,
    "productQuantities": Vector <int>
}
```

## Baking Notification

#### Content

```
{
    "productType": String,
    "guids": Vector<String>,
    "productQuantities": Vector <int>
}
```

## Preparation Request
```
{
    "productQuantities": Vector <int>,
    "steps": Array JSON Objects,
    "productType": String,
    "guids": Vector<String>
}
```
Step
```
{
    "action": String,
    "duration": float
}
```

## Preparation Notification
```
{
    "guids": Vector<String>
    "productType": String,
}
```  

## Cooling Request

#### Content

```
[
    {
        guid: String("product_name"),
        quantity: int(quantity),
        coolingDuration: int(cooling_duration)
    }
]
```

## Loading Bay Message

#### Content

```

"products": [
    {
        String("product_name"): int(quantity),
    }
    {
        String("product_name"): int(quantity),
    }

]

```
