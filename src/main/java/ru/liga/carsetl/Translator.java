package ru.liga.carsetl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.liga.carsetl.domain.Car;
import ru.liga.carsetl.domain.Suv;
import ru.liga.carsetl.domain.Truck;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
@Component
public class Translator {
    public final static String CAR_FOLDER = "C:\\carsetl\\src\\main\\etl\\cars";
    public final static String SUV_FOLDER = "C:\\carsetl\\src\\main\\etl\\suvs";
    public final static String TRUCK_FOLDER = "C:\\carsetl\\src\\main\\etl\\trucks";


    public static Stream<Tuple2<Car, File>> readDirectory(Path directory) throws IOException, JAXBException {
        Unmarshaller unmarshaller = getUnmarshaller();
        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .peek(p -> log.info("Parsing " + p))
                .flatMap(file ->
                        Try.of(() ->
                                Tuple.of((Car) unmarshaller.unmarshal(file), file)
                        ).toJavaStream()
                );
    }

    public static Stream<Either<Suv, Truck>> partitionCars(Stream<Tuple2<Car, File>> stream) {
        return stream.flatMap(tuple2 -> {
                    Car car = tuple2._1;
                    File file = tuple2._2;
                    if (car.getType().equals("truck")) {
                        file.delete();
                        return Stream.of(Either.right(new Truck(car)));
                    }
                    if (car.getType().equals("suv")) {
                        file.delete();
                        return Stream.of(Either.left(new Suv(car)));
                    }
                    return Stream.empty();
                }
        );
    }

    public static void saveAsJson(Path suvDir, Path truckDir, Stream<Either<Suv, Truck>> stream) {
        ObjectMapper mapper = new ObjectMapper();
        stream.forEach(ei -> {
                    Object car;
                    File file;
                    if (ei.isLeft()) {
                        car = ei.getLeft();
                        file = new File(suvDir.toFile(), ei.getLeft().getVin() + ".json");
                    } else {
                        car = ei.get();
                        file = new File(truckDir.toFile(), ei.get().getVin() + ".json");
                    }
                    try {
                        mapper.writeValue(file, car);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @Scheduled(fixedDelay = 20_000L)
    public void etl() {
        log.info("Started parsing cars folder");
        try {
            val cars = readDirectory(Paths.get(CAR_FOLDER));
            val eithers = partitionCars(cars);
            Path suvPath = Paths.get(SUV_FOLDER);
            Path truckPath = Paths.get(TRUCK_FOLDER);
            saveAsJson(suvPath, truckPath, eithers);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private static Unmarshaller getUnmarshaller() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Car.class);
        return context.createUnmarshaller();
    }

    public static void main(String[] args) throws IOException, JAXBException {
//        Stream<Path> paths = Files.walk(Paths.get(CAR_FOLDER)).filter(Files::isRegularFile);
//        paths.forEach(path -> {
//            try {
//                Files.deleteIfExists(path);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        });
        new Translator().etl();
    }
}
