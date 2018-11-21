# Messages used for the Baking Stage

## Dough Notification Messages

#### Content

```
{
    "productType": String,
    "guids": Vector<String>
}
```

## Baking Request

#### Content

```
{
    "bakingTemp": int,
    "bakingTime": float,
    "productType": String,
    "guids": Vector<String>
}

```

## Baking Notification

#### Content

```
{
    "productType": String,
    "guids": Vector<String>
}


```

## Cooling Request

#### Content
```
{
    "productName": String,
    "coolingRate": int,
    "quantity": int,
    "boxingTemperature": int
}

```

## Loading Bay Message 

#### Content

```
{
    "products": {
      String("product_name"): int(quantity),
      String("another_product_name"): int(quantity),
      ...
    }
}
```
