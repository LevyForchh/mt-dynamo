package com.salesforce.dynamodbv2;

import static com.amazonaws.services.dynamodbv2.model.ScalarAttributeType.S;
import static com.salesforce.dynamodbv2.testsupport.DefaultTestSetup.TABLE1;
import static com.salesforce.dynamodbv2.testsupport.DefaultTestSetup.TABLE3;
import static com.salesforce.dynamodbv2.testsupport.ItemBuilder.HASH_KEY_FIELD;
import static com.salesforce.dynamodbv2.testsupport.ItemBuilder.INDEX_FIELD;
import static com.salesforce.dynamodbv2.testsupport.ItemBuilder.RANGE_KEY_FIELD;
import static com.salesforce.dynamodbv2.testsupport.ItemBuilder.SOME_FIELD;
import static com.salesforce.dynamodbv2.testsupport.TestSupport.HASH_KEY_VALUE;
import static com.salesforce.dynamodbv2.testsupport.TestSupport.INDEX_FIELD_VALUE;
import static com.salesforce.dynamodbv2.testsupport.TestSupport.RANGE_KEY_S_VALUE;
import static com.salesforce.dynamodbv2.testsupport.TestSupport.SOME_FIELD_VALUE;
import static com.salesforce.dynamodbv2.testsupport.TestSupport.createAttributeValue;
import static com.salesforce.dynamodbv2.testsupport.TestSupport.createStringAttribute;
import static com.salesforce.dynamodbv2.testsupport.TestSupport.getItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.google.common.collect.ImmutableMap;
import com.salesforce.dynamodbv2.mt.mappers.sharedtable.impl.MtAmazonDynamoDbBySharedTable;
import com.salesforce.dynamodbv2.testsupport.ArgumentBuilder.TestArgument;
import com.salesforce.dynamodbv2.testsupport.DefaultArgumentProvider;
import com.salesforce.dynamodbv2.testsupport.ItemBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests updateItem().
 *
 * @author msgroi
 */
class UpdateTest {

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentProvider.class)
    void update(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            Map<String, AttributeValue> updateItemKey = ItemBuilder.builder(testArgument.getHashKeyAttrType(),
                        HASH_KEY_VALUE)
                    .build();
            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(TABLE1)
                .withKey(updateItemKey)
                .withUpdateExpression("set #someField = :someValue")
                .withExpressionAttributeNames(ImmutableMap.of("#someField", SOME_FIELD))
                .withExpressionAttributeValues(ImmutableMap.of(":someValue",
                    createStringAttribute(SOME_FIELD_VALUE + TABLE1 + org + "Updated")));
            testArgument.getAmazonDynamoDb().updateItem(updateItemRequest);
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE1 + org + "Updated")
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE1,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.empty()));
            assertEquals(new HashMap<>(updateItemKey), updateItemRequest.getKey()); // assert no side effects
            assertEquals(TABLE1, updateItemRequest.getTableName()); // assert no side effects
        });
    }

    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateConditionalSuccess(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            testArgument.getAmazonDynamoDb().updateItem(new UpdateItemRequest()
                .withTableName(TABLE1)
                .withKey(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                        .build())
                .withUpdateExpression("set #someField = :newValue")
                .withConditionExpression("#someField = :currentValue")
                .addExpressionAttributeNamesEntry("#someField", SOME_FIELD)
                .addExpressionAttributeValuesEntry(":currentValue",
                    createStringAttribute(SOME_FIELD_VALUE + TABLE1 + org))
                .addExpressionAttributeValuesEntry(":newValue",
                    createStringAttribute(SOME_FIELD_VALUE + TABLE1 + org + "Updated")));
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE1 + org + "Updated")
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE1,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.empty()));
        });
    }

    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateConditionalOnHkSuccess(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            testArgument.getAmazonDynamoDb().updateItem(new UpdateItemRequest()
                .withTableName(TABLE1)
                .withKey(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                        .build())
                .withUpdateExpression("set #someField = :newValue")
                .withConditionExpression("#hk = :currentValue")
                .addExpressionAttributeNamesEntry("#someField", SOME_FIELD)
                .addExpressionAttributeNamesEntry("#hk", HASH_KEY_FIELD)
                .addExpressionAttributeValuesEntry(":currentValue",
                    createAttributeValue(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE))
                .addExpressionAttributeValuesEntry(":newValue",
                    createStringAttribute(SOME_FIELD_VALUE + TABLE1 + org + "Updated")));
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE1 + org + "Updated")
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE1,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.empty()));
        });
    }


    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateConditionalOnGsiHkSuccess(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            testArgument.getAmazonDynamoDb().updateItem(new UpdateItemRequest()
                .withTableName(TABLE3)
                .withKey(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                    .rangeKey(S, RANGE_KEY_S_VALUE)
                    .build())
                .withUpdateExpression("set #indexField = :newValue")
                .withExpressionAttributeNames(ImmutableMap.of("#indexField", INDEX_FIELD))
                .withExpressionAttributeValues(ImmutableMap.of(":newValue",
                    createStringAttribute(INDEX_FIELD_VALUE + TABLE3 + org + "Updated"))));
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE3 + org)
                .indexField(S, INDEX_FIELD_VALUE + TABLE3 + org + "Updated")
                .rangeKey(S, RANGE_KEY_S_VALUE)
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE3,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.of(RANGE_KEY_S_VALUE)));
        });
    }

    @ParameterizedTest(name = "{arguments}")
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateConditionalOnHkWithLiteralsSuccess(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            testArgument.getAmazonDynamoDb().updateItem(new UpdateItemRequest()
                .withTableName(TABLE1)
                .withKey(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                        .build())
                .withUpdateExpression("set " + SOME_FIELD + " = :newValue")
                .withConditionExpression(HASH_KEY_FIELD + " = :currentValue")
                .addExpressionAttributeValuesEntry(":currentValue",
                    createAttributeValue(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE))
                .addExpressionAttributeValuesEntry(":newValue",
                    createStringAttribute(SOME_FIELD_VALUE + TABLE1 + org + "Updated")));
            assertEquals(ItemBuilder
                .builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE1 + org + "Updated")
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                TABLE1,
                HASH_KEY_VALUE,
                testArgument.getHashKeyAttrType(),
                Optional.empty()));
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateConditionalFail(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            try {
                testArgument.getAmazonDynamoDb().updateItem(new UpdateItemRequest()
                    .withTableName(TABLE1)
                    .withKey(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                            .build())
                    .withUpdateExpression("set #name = :newValue")
                    .withConditionExpression("#name = :currentValue")
                    .addExpressionAttributeNamesEntry("#name", SOME_FIELD)
                    .addExpressionAttributeValuesEntry(":currentValue", createStringAttribute("invalidValue"))
                    .addExpressionAttributeValuesEntry(":newValue",
                        createStringAttribute(SOME_FIELD_VALUE + TABLE1 + org + "Updated")));
                throw new RuntimeException("expected ConditionalCheckFailedException was not encountered");
            } catch (ConditionalCheckFailedException e) {
                assertTrue(e.getMessage().contains("ConditionalCheckFailedException"));
            }
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE1 + org)
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE1,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.empty()));
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateHkRkTable(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            Map<String, AttributeValue> updateItemKey = ItemBuilder.builder(testArgument.getHashKeyAttrType(),
                    HASH_KEY_VALUE)
                    .rangeKey(S, RANGE_KEY_S_VALUE)
                    .build();
            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(TABLE3)
                .withKey(updateItemKey)
                .withUpdateExpression("set #someField = :someValue")
                .withExpressionAttributeNames(ImmutableMap.of("#someField", SOME_FIELD))
                .withExpressionAttributeValues(ImmutableMap.of(":someValue",
                    createStringAttribute(SOME_FIELD_VALUE + TABLE3 + org + "Updated")));
            testArgument.getAmazonDynamoDb().updateItem(updateItemRequest);
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE3 + org + "Updated")
                .rangeKey(S, RANGE_KEY_S_VALUE)
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE3,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.of(RANGE_KEY_S_VALUE)));
            assertEquals(new HashMap<>(updateItemKey), updateItemRequest.getKey()); // assert no side effects
            assertEquals(TABLE3, updateItemRequest.getTableName()); // assert no side effects
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateHkRkTableWithGsi(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            Map<String, AttributeValue> updateItemKey = ItemBuilder.builder(testArgument.getHashKeyAttrType(),
                HASH_KEY_VALUE)
                .rangeKey(S, RANGE_KEY_S_VALUE)
                .build();
            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(TABLE3)
                .withKey(updateItemKey)
                .withUpdateExpression("set #indexField = :indexValue, #someField = :someValue")
                .withExpressionAttributeNames(ImmutableMap.of(
                    "#indexField", INDEX_FIELD,
                    "#someField", SOME_FIELD))
                .withExpressionAttributeValues(ImmutableMap.of(
                    ":indexValue", createStringAttribute(INDEX_FIELD_VALUE + TABLE3 + org + "Updated"),
                    ":someValue", createStringAttribute(SOME_FIELD_VALUE + TABLE3 + org + "Updated")));
            testArgument.getAmazonDynamoDb().updateItem(updateItemRequest);
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE3 + org + "Updated")
                .indexField(S, INDEX_FIELD_VALUE + TABLE3 + org + "Updated")
                .rangeKey(S, RANGE_KEY_S_VALUE)
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE3,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.of(RANGE_KEY_S_VALUE)));
            assertEquals(new HashMap<>(updateItemKey), updateItemRequest.getKey()); // assert no side effects
            assertEquals(TABLE3, updateItemRequest.getTableName()); // assert no side effects
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateConditionalSuccessHkRkTable(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            testArgument.getAmazonDynamoDb().updateItem(new UpdateItemRequest()
                .withTableName(TABLE3)
                .withKey(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                        .rangeKey(S, RANGE_KEY_S_VALUE)
                        .build())
                .withUpdateExpression("set #someField = :newValue")
                .withConditionExpression("#someField = :currentValue")
                .addExpressionAttributeNamesEntry("#someField", SOME_FIELD)
                .addExpressionAttributeValuesEntry(":currentValue", createStringAttribute(SOME_FIELD_VALUE
                    + TABLE3 + org))
                .addExpressionAttributeValuesEntry(":newValue",
                    createStringAttribute(SOME_FIELD_VALUE + TABLE3 + org + "Updated")));
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE3 + org + "Updated")
                .rangeKey(S, RANGE_KEY_S_VALUE)
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE3,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.of(RANGE_KEY_S_VALUE)));
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateConditionalOnHkRkSuccessHkRkTable(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            testArgument.getAmazonDynamoDb().updateItem(new UpdateItemRequest()
                .withTableName(TABLE3)
                .withKey(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                        .rangeKey(S, RANGE_KEY_S_VALUE)
                        .build())
                .withUpdateExpression("set #someField = :newValue")
                .withConditionExpression("#hk = :currentHkValue and #rk = :currentRkValue")
                .addExpressionAttributeNamesEntry("#someField", SOME_FIELD)
                .addExpressionAttributeNamesEntry("#hk", HASH_KEY_FIELD)
                .addExpressionAttributeNamesEntry("#rk", RANGE_KEY_FIELD)
                .addExpressionAttributeValuesEntry(":currentHkValue",
                    createAttributeValue(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE))
                .addExpressionAttributeValuesEntry(":currentRkValue", createStringAttribute(RANGE_KEY_S_VALUE))
                .addExpressionAttributeValuesEntry(":newValue",
                    createStringAttribute(SOME_FIELD_VALUE + TABLE3 + org + "Updated")));
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE3 + org + "Updated")
                .rangeKey(S, RANGE_KEY_S_VALUE)
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE3,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.of(RANGE_KEY_S_VALUE)));
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentProvider.class)
    void updateConditionalFailHkRkTable(TestArgument testArgument) {
        testArgument.forEachOrgContext(org -> {
            try {
                testArgument.getAmazonDynamoDb().updateItem(new UpdateItemRequest()
                    .withTableName(TABLE3)
                    .withKey(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                            .rangeKey(S, RANGE_KEY_S_VALUE)
                            .build())
                    .withUpdateExpression("set #name = :newValue")
                    .withConditionExpression("#name = :currentValue")
                    .addExpressionAttributeNamesEntry("#name", SOME_FIELD)
                    .addExpressionAttributeValuesEntry(":currentValue", createStringAttribute("invalidValue"))
                    .addExpressionAttributeValuesEntry(":newValue", createStringAttribute(SOME_FIELD_VALUE
                        + TABLE3 + org + "Updated")));
                throw new RuntimeException("expected ConditionalCheckFailedException was not encountered");
            } catch (ConditionalCheckFailedException e) {
                assertTrue(e.getMessage().contains("ConditionalCheckFailedException"));
            }
            assertEquals(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE)
                .someField(S, SOME_FIELD_VALUE + TABLE3 + org)
                .rangeKey(S, RANGE_KEY_S_VALUE)
                .build(),
                getItem(testArgument.getAmazonDynamoDb(),
                    TABLE3,
                    HASH_KEY_VALUE,
                    testArgument.getHashKeyAttrType(),
                    Optional.of(RANGE_KEY_S_VALUE)));
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentProvider.class)
    void attributeUpdatesNotSupportedInSharedTable(TestArgument testArgument) {
        if (testArgument.getAmazonDynamoDb() instanceof MtAmazonDynamoDbBySharedTable) {
            try {
                testArgument.getAmazonDynamoDb().updateItem(new UpdateItemRequest()
                    .withTableName(TABLE1)
                    .withKey(ItemBuilder.builder(testArgument.getHashKeyAttrType(), HASH_KEY_VALUE).build())
                    .addAttributeUpdatesEntry(SOME_FIELD,
                        new AttributeValueUpdate()
                            .withValue(createStringAttribute(SOME_FIELD_VALUE + TABLE1 + "Updated"))));
                fail("expected IllegalArgumentException not encountered");
            } catch (IllegalArgumentException e) {
                assertEquals(
                    "Use of attributeUpdates in UpdateItemRequest objects is not supported.  "
                        + "Use UpdateExpression instead.",
                    e.getMessage());
            }
        }
    }

}
