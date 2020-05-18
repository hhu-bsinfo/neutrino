package de.hhu.bsinfo.neutrino.example.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PhoneBook {

//    private static final Faker FAKER = new Faker();
//
//    private static final Schema SCHEMA = new Schema(List.of(
//            new Field("name", FieldType.nullable(new ArrowType.Utf8()), null),
//            new Field("street", FieldType.nullable(new ArrowType.Utf8()), null),
//            new Field("zip", FieldType.nullable(new ArrowType.Utf8()), null),
//            new Field("city", FieldType.nullable(new ArrowType.Utf8()), null),
//            new Field("country", FieldType.nullable(new ArrowType.Utf8()), null),
//            new Field("telephone", FieldType.nullable(new ArrowType.Utf8()), null)
//    ));
//
//    private final VectorSchemaRoot root;
//
//    private final VarCharVector name;
//    private final VarCharVector street;
//    private final VarCharVector zip;
//    private final VarCharVector city;
//    private final VarCharVector country;
//    private final VarCharVector telephone;
//
//    private PhoneBook(VectorSchemaRoot root) {
//        this.root = root;
//        name = (VarCharVector) root.getVector("name");
//        street = (VarCharVector) root.getVector("street");
//        zip = (VarCharVector) root.getVector("zip");
//        city = (VarCharVector) root.getVector("city");
//        country = (VarCharVector) root.getVector("country");
//        telephone = (VarCharVector) root.getVector("telephone");
//    }
//
//    private PhoneBook(BaseAllocator allocator) {
//        this(VectorSchemaRoot.create(SCHEMA, allocator));
//    }
//
//    public static PhoneBook create(BaseAllocator allocator, int entries) {
//        var phoneBook = new PhoneBook(allocator);
//
//        phoneBook.root.setRowCount(entries);
//        phoneBook.root.getFieldVectors().forEach(vector -> {
//            vector.setInitialCapacity(entries);
//            vector.allocateNew();
//        });
//
//        var text = new Text();
//        for (int i = 0; i < entries; i++) {
//            text.set(FAKER.name().fullName());
//            phoneBook.name.setSafe(i, text);
//
//            text.set(FAKER.address().streetName());
//            phoneBook.street.setSafe(i, text);
//
//            text.set(FAKER.address().zipCode());
//            phoneBook.zip.setSafe(i, text);
//
//            text.set(FAKER.address().city());
//            phoneBook.city.setSafe(i, text);
//
//            text.set(FAKER.address().country());
//            phoneBook.country.setSafe(i, text);
//
//            text.set(FAKER.phoneNumber().phoneNumber());
//            phoneBook.telephone.setSafe(i, text);
//        }
//
//        phoneBook.root.getFieldVectors().forEach(vector -> {
//            vector.setValueCount(entries);
//        });
//
//        return phoneBook;
//    }
//
//    public List<FieldVector> getVectors() {
//        return root.getFieldVectors();
//    }
//
//    public Schema getSchema() {
//        return SCHEMA;
//    }
//
//    public int getRowCount() {
//        return root.getRowCount();
//    }
//
//    public long sizeInBytes() {
//        return root.getFieldVectors().stream()
//                .mapToLong(vector -> vector.getDataBuffer().capacity() +
//                                     vector.getValidityBuffer().capacity() +
//                                     vector.getOffsetBuffer().capacity()).sum();
//    }
//
//
//
//    @Override
//    public String toString() {
//        return root.contentToTSVString();
//    }
//
//    private Mono<Void> read(InfinibandSocket socket, VarCharVector vector, FieldMeta meta) {
//        var device = socket.getDevice();
//
//        // Get vector buffers
//        var dataBuffer = vector.getDataBuffer();
//        var validityBuffer = vector.getValidityBuffer();
//        var offsetBuffer = vector.getOffsetBuffer();
//
//        if (dataBuffer.capacity() < meta.getDataLength()) {
//            log.warn("Local data buffer capacity is less than remote capacity");
//        }
//
//        if (validityBuffer.capacity() < meta.getValidityLength()) {
//            log.warn("Local validity buffer capacity is less than remote capacity");
//        }
//
//        if (offsetBuffer.capacity() < meta.getOffsetLength()) {
//            log.warn("Local offset buffer capacity is less than remote capacity");
//        }
//
//        // Create data buffer operation
////        var dataRegion = device.wrapRegion(dataBuffer.memoryAddress(), dataBuffer.capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
////        var localDataHandle = new LocalHandle(dataRegion.getAddress(), (int) dataRegion.getLength(), dataRegion.getLocalKey());
////        var remoteDataHandle = new RemoteHandle(meta.getDataAddress(), meta.getDataKey());
////        var dataOperation = socket.read(localDataHandle, remoteDataHandle);
////
////        // Create validity buffer operation
////        var validityRegion = device.wrapRegion(validityBuffer.memoryAddress(), validityBuffer.capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
////        var localValidityHandle = new LocalHandle(validityRegion.getAddress(), (int) validityRegion.getLength(), validityRegion.getLocalKey());
////        var remoteValidityHandle = new RemoteHandle(meta.getValidityAddress(), meta.getValidityKey());
////        var validityOperation = socket.read(localValidityHandle, remoteValidityHandle);
////
////        // Create offset buffer operation
////        var offsetRegion = device.wrapRegion(offsetBuffer.memoryAddress(), offsetBuffer.capacity(), MemoryRegion.DEFAULT_ACCESS_FLAGS);
////        var localOffsetHandle = new LocalHandle(offsetRegion.getAddress(), (int) offsetRegion.getLength(), offsetRegion.getLocalKey());
////        var remoteOffsetHandle = new RemoteHandle(meta.getOffsetAddress(), meta.getOffsetKey());
////        var offsetOperation = socket.read(localOffsetHandle, remoteOffsetHandle);
////
////        return validityOperation.and(offsetOperation).and(dataOperation);
//
//        return null;
//    }
//
//    public static Mono<PhoneBook> read(InfinibandSocket socket, VectorMeta vectorMeta, BaseAllocator allocator) {
//        var phoneBook = new PhoneBook(allocator);
//
//        // Set row count and allocate buffers
//        phoneBook.root.setRowCount(vectorMeta.getRowCount());
//        phoneBook.root.getFieldVectors().forEach(vector -> {
//            var meta = vectorMeta.getFieldsOrThrow(vector.getName());
//            vector.setInitialCapacity(vectorMeta.getRowCount());
//            ((VariableWidthVector) vector).allocateNew(meta.getDataLength(), vectorMeta.getRowCount());
//            vector.setValueCount(vectorMeta.getRowCount());
//        });
//
//        // Create RDMA Read operations for all vectors
//        var readOperation = phoneBook.root.getFieldVectors().stream()
//                .map(vector -> phoneBook.read(socket, (VarCharVector) vector, vectorMeta.getFieldsOrThrow(vector.getName())))
//                .reduce(Mono::and)
//                .orElseThrow();
//
//        return readOperation.thenReturn(phoneBook);
//    }
//
//    public void writeTo(File output) {
//        try (var outputStream = new FileOutputStream(output);
//             var channel = outputStream.getChannel();
//             var writer = new ArrowFileWriter(root, new DictionaryProvider.MapDictionaryProvider(), channel)){
//
//            writer.start();
//            writer.writeBatch();
//            writer.end();
//
//        } catch (IOException e) {
//            log.error("", e);
//        }
//    }
}
