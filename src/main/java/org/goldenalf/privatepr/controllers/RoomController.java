package org.goldenalf.privatepr.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.goldenalf.privatepr.dto.BookDateDto;
import org.goldenalf.privatepr.dto.RoomDto;
import org.goldenalf.privatepr.models.Hotel;
import org.goldenalf.privatepr.models.Room;
import org.goldenalf.privatepr.services.HotelService;
import org.goldenalf.privatepr.services.RoomService;
import org.goldenalf.privatepr.utils.erorsHandler.ErrorHandler;
import org.goldenalf.privatepr.utils.exeptions.HotelErrorException;
import org.goldenalf.privatepr.utils.exeptions.RoomErrorException;
import org.goldenalf.privatepr.utils.erorsHandler.validator.RoomValidator;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;


@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final HotelService hotelService;
    private final ModelMapper modelMapper;
    private final RoomValidator roomValidator;
    private final MessageSource messageSource;

    @GetMapping("/{id_room}")
    public RoomDto getRoom(@PathVariable("id_room") int id) {
        Room room = roomService.getRoom(id).orElseThrow(() -> new RoomErrorException(messageSource
                .getMessage("validation.hotelBook.room.exception.room-not-found", null, Locale.getDefault())));
        return convertToRoomDto(room);
    }

    @GetMapping("/{id_hotel}/allRooms")
    public List<RoomDto> getAllRoomsInHotel(@PathVariable("id_hotel") int hotelId) {
        return convertToRoomDtoList(roomService.findAllRoomsByHotelId(hotelId));
    }

    @PutMapping("/allAvailableRoomsForGivenDate")
    public List<RoomDto> getAllAvailableRoomsInHotelForGivenDate(@RequestBody @Valid BookDateDto bookDateDto) {
        return roomService.findAllRoomsInHotelForGivenDate(bookDateDto, true);
    }

    @PutMapping("/allBookedRoomsForGivenDate")
    public List<RoomDto> getAllBookedRoomsInHotelForGivenDate(@RequestBody @Valid BookDateDto bookDateDto) {
        return roomService.findAllRoomsInHotelForGivenDate(bookDateDto, false);
    }

    @PostMapping("/{id_hotel}/new")
    public ResponseEntity<HttpStatus> saveRoom(@RequestBody @Valid RoomDto roomDto,
                                               @PathVariable("id_hotel") int hotelId,
                                               BindingResult bindingResult) {

        Hotel hotel = hotelService.getHotel(hotelId).orElseThrow(() -> new HotelErrorException(messageSource.
                getMessage("validation.hotelBook.hotel.exception.hotel-not-found", null, Locale.getDefault())));
        Room room = convertToRoom(roomDto);
        room.setHotel(hotel);

        roomValidator.validate(room, bindingResult);

        if (bindingResult.hasErrors()) {
            throw new RoomErrorException(ErrorHandler.getErrorMessage(bindingResult));
        }

        roomService.save(room);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @PatchMapping("/{id_room}")
    public ResponseEntity<HttpStatus> updateRoom(@PathVariable("id_room") int id,
                                                 @RequestBody @Valid RoomDto roomDto,
                                                 BindingResult bindingResult) {
        Room updatedRoom = convertToRoom(roomDto);
        Room room = roomService.getRoom(id).orElseThrow(() -> new RoomErrorException(messageSource
                .getMessage("validation.hotelBook.room.exception.room-not-found", null, Locale.getDefault())));

        //Задаю отель и id для обновлённой комнаты, нужно для валидатора
        updatedRoom.setHotel(room.getHotel());
        updatedRoom.setId(room.getId());
        roomValidator.validate(updatedRoom, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new RoomErrorException(ErrorHandler.getErrorMessage(bindingResult));
        }

        roomService.update(id, updatedRoom);
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @DeleteMapping("/{id_room}")
    public ResponseEntity<HttpStatus> deleteRoom(@PathVariable("id_room") int id) {
        roomService.delete(id);
        return ResponseEntity.ok(HttpStatus.OK);
    }


    private RoomDto convertToRoomDto(Room room) {
        return modelMapper.map(room, RoomDto.class);
    }

    private List<RoomDto> convertToRoomDtoList(List<Room> room) {
        Type listType = new TypeToken<List<RoomDto>>() {
        }.getType();
        return modelMapper.map(room, listType);
    }

    private Room convertToRoom(RoomDto room) {
        return modelMapper.map(room, Room.class);
    }
}
