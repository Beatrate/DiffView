using System;
using System.Net;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AutoMapper;
using Trainload.Models;
using Trainload.DataLayer;
using Trainload.Helpers;

namespace Trainload.Controllers
{
	[Produces("application/json")]
	[Route("api/[controller]")]
	public class RoutesController : ControllerBase
	{
		private readonly IMapper mapper;
		private readonly TrainloadContext context;

		public RoutesController(IMapper mapper, TrainloadContext context)
		{
			this.mapper = mapper;
			this.context = context;
		}

		/// <summary>
		/// Finds all routes from the destination to the location in specified time.
		/// </summary>
		/// <param name="origin">Name of origin location.</param>
		/// <param name="destination">Name of destination location.</param>
		/// <param name="date">Date of departure. Time is ignored.</param>
		[HttpGet("{origin}/{destination}/{date}")]
		[ProducesResponseType(400)]
		[ProducesResponseType(typeof(RouteCatalogDTO), 200)]
		public IActionResult GetRoutes(string origin, string destination, DateTime date)
		{
			if(!ModelState.IsValid)
			{
				return BadRequest(ModelState);
			}
			var query = from route in context.Routes
						.Include(route => route.OriginStation)
							.ThenInclude(station => station.Location)
						.Include(route => route.DestinationStation)
							.ThenInclude(station => station.Location)
						.Include(route => route.Train)
						where route.OriginStation.Location.Name == origin && route.DestinationStation.Location.Name == destination
						&& route.DepartureTime.Date == date.Date
						select route;
			//var routes = from route in context.Routes
			//			 join originStation in context.Stations on route.OriginStationId equals originStation.Id
			//			 join originLocation in context.Locations on originStation.LocationId equals originLocation.Id
			//			 join destinationStation in context.Stations on route.DestinationStationId equals destinationStation.Id
			//			 join destinationLocation in context.Locations on destinationStation.LocationId equals destinationLocation.Id
			//			 where originLocation.Name == origin && destinationLocation.Name == destination
			//			 select route;
			var queried = query.AsEnumerable();

			var routes = new List<RouteDTO>();
			var stations = new List<StationDTO>();
			var locations = new List<LocationDTO>();
			if(queried.Any())
			{
				Route first = queried.First();
				locations.Add(mapper.Map<LocationDTO>(first.OriginStation.Location));
				locations.Add(mapper.Map<LocationDTO>(first.DestinationStation.Location));
			}
			var trains = new List<TrainDTO>();
			foreach(Route route in queried)
			{
				routes.Add(mapper.Map<RouteDTO>(route));
				if(!stations.Exists(station => station.Id == route.OriginStationId))
				{
					stations.Add(mapper.Map<StationDTO>(route.OriginStation));
				}
				if(!stations.Exists(station => station.Id == route.DestinationStationId))
				{
					stations.Add(mapper.Map<StationDTO>(route.DestinationStation));
				}
				if(!trains.Exists(train => train.Id == route.TrainId))
				{
					trains.Add(mapper.Map<TrainDTO>(route.Train));
				}
			}

			var catalog = new RouteCatalogDTO { Routes = routes, Stations = stations, Locations = locations, Trains = trains };
			return Ok(catalog);
		}

		/// <summary>
		/// Gets booking data for a route.
		/// </summary>
		/// <param name="id">Route id.</param>
		[HttpGet("{id}")]
		[ProducesResponseType(400)]
		[ProducesResponseType(404)]
		[ProducesResponseType(typeof(RouteStateDTO), 200)]
		public async Task<IActionResult> GetRoute(int id)
		{
			if(!ModelState.IsValid)
			{
				return BadRequest(ModelState);
			}

			Route route = await context.Routes.FindAsync(id);
			if(route == null)
			{
				return NotFound();
			}

			var bookedCars =
				from car in context.Cars
				where car.TrainId == route.TrainId
				join booking in context.Bookings on car.Id equals booking.CarId into carGroup
				from carBooking in carGroup.DefaultIfEmpty()
				select new { Car = car, Bookings = carGroup };

			var stateCars = new List<CarDTO>();
			foreach(var bookedCar in bookedCars)
			{
				CarDTO car = mapper.Map<CarDTO>(bookedCar.Car);
				var occupied = new List<int>();
				foreach(Booking booking in bookedCar.Bookings)
				{
					occupied.Add(booking.Seat);
				}
				car.OccupiedSeats = occupied;
				stateCars.Add(car);
			}

			return Ok(new RouteStateDTO() { Route = mapper.Map<RouteDTO>(route), Cars = stateCars });
		}
	}
}
