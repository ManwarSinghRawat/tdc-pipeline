package com.github.rmpestano.tdc.cars.rest;


import java.util.List;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import com.github.adminfaces.persistence.model.Filter;
import com.github.rmpestano.tdc.cars.service.CarService;
import com.github.rmpestano.tdc.cars.infra.security.RestSecured;
import com.github.rmpestano.tdc.cars.model.Car;
import com.github.rmpestano.tdc.cars.model.Car_;

@Path("/cars")
@Produces("application/json;charset=utf-8")
public class CarEndpoint {

    @Inject
    CarService carService;

    /**
     * Creates a new car
     */
    @POST
    @Consumes("application/json")
    public Response create(Car entity) {
        carService.insert(entity);
        return Response.created(UriBuilder.fromResource(CarEndpoint.class).path(String.valueOf(entity.getId())).build()).build();
    }

    /**
     * Deletes a car based on its ID
     * @param user name of the user to log in
     * @param id car ID
     * @status 401 User not authorized
     * @status 403 User not authenticated
     */
    @DELETE
    @Path("/{id:[0-9][0-9]*}")
    @RestSecured //javaee 7 filters
    public Response deleteById(@HeaderParam("user") String user, @PathParam("id") Integer id) {
        Car entity = carService.findById(id);
        if (entity == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        carService.remove(entity);
        return Response.noContent().build();
    }

    /**
     * Finds a car based on its ID
     * @param id car ID
     */
    @GET
    @Path("/{id:[0-9][0-9]*}")
    public Response findById(@PathParam("id") Integer id, @Context Request request,@Context HttpHeaders headers) {
        Car entity;

        try {
            entity = carService.findById(id);
        } catch (NoResultException nre) {
            entity = null;
        }

        if(entity == null){
            return Response.status(Status.NOT_FOUND).build();
        }

        CacheControl cc = new CacheControl();
        cc.setMaxAge(100);
        EntityTag tag = new EntityTag(Integer.toString(entity.hashCode()));
        Response.ResponseBuilder builder =  request.evaluatePreconditions(tag);
        if(builder != null){
            builder.cacheControl(cc);
            return builder.build();
        }
        builder = Response.ok(entity);
        builder.cacheControl(cc);
        builder.tag(tag);
        return builder.build();
    }

    /**
     * @param startPosition initial list position
     * @param maxResult number of elements to retrieve
     * @param minPrice minimum car price
     * @param maxPrice maximum car price
     * @param model list cars with given model
     * @param name list cars with given name
     */
    @GET
    public Response list(@QueryParam("start") @DefaultValue("0") Integer startPosition,
                          @QueryParam("max") @DefaultValue("10") Integer maxResult,
                          @QueryParam("model") @DefaultValue("") String model,
                          @QueryParam("name") @DefaultValue("") String name,
                          @QueryParam("minPrice") @DefaultValue("0") Double minPrice,
                          @QueryParam("maxPrice") @DefaultValue("20000") Double maxPrice) {
        Filter<Car> filter = new Filter<>();
        Car car = new Car();
        filter.setEntity(car);
        if(model != null){
            filter.getEntity().model(model);
        }
        if(name != null){
            filter.getEntity().name(name);
        }
        if(minPrice != null){
          filter.addParam("minPrice",minPrice);
        }
       if(maxPrice != null){
         filter.addParam("maxPrice",maxPrice);

       }
       filter.setFirst(startPosition).setPageSize(maxResult);
       final List<Car> results = carService.paginate(filter);

       return Response.ok(results).header("count",carService.count(filter)).build();
    }

    /**
     * Counts number of cars
     */
    @GET
    @Path("count")
    public Response count(@QueryParam("model") String model,
                            @QueryParam("name") String name) {
        Filter<Car> filter = new Filter<>();
        Car car = new Car();
        filter.setEntity(car);
        if(model != null){
            filter.getEntity().model(model);
        }
        if(name != null){
            filter.getEntity().name(name);
        }
        return Response.ok(carService.count(filter)).build();
    }

    /**
     *
     * @param id The identifier of the car to be updated
     * @param entity the changes to be applied
     */
    @PUT
    @Path("/{id:[0-9][0-9]*}")
    @Consumes("application/json")
    public Response update(@PathParam("id") Integer id,  Car entity) {
        if (entity == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        if (!id.equals(entity.getId())) {
            return Response.status(Status.CONFLICT).entity(entity).build();
        }
        if (carService.count(carService.criteria().eq(Car_.id,id)) == 0) {
            return Response.status(Status.NOT_FOUND).build();
        }
        try {
            carService.update(entity);
        } catch (OptimisticLockException e) {
            return Response.status(Status.CONFLICT).entity(e.getEntity()).build();
        }

        return Response.noContent().build();
    }
}
