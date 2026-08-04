package com.batdan.valyrainsteel.client.effect;

public class LightningExplosionEffect {


    public static void create(
            ClientLevel level,
            Vec3 pos
    ){



// CORE FLASH

        for(int i=0;i<150;i++){

            double x =
                    (level.random.nextDouble()-0.5)*6;

            double y =
                    (level.random.nextDouble())*8;

            double z =
                    (level.random.nextDouble()-0.5)*6;


            level.addParticle(
                    ModParticles.LIGHTNING_EXPLOSION.get(),
                    pos.x+x,
                    pos.y+y,
                    pos.z+z,
                    0,
                    0.05,
                    0
            );

        }



// LIGHTNING ARMS

        for(int bolt=0;bolt<16;bolt++){


            double angle =
                    level.random.nextDouble()*Math.PI*2;


            Vec3 end =
                    pos.add(
                            Math.cos(angle)*10,
                            level.random.nextDouble()*8,
                            Math.sin(angle)*10
                    );



            createBolt(
                    level,
                    pos,
                    end
            );


        }



// ENERGY RING

        for(int i=0;i<100;i++){


            double angle =
                    Math.PI*2*i/100;


            level.addParticle(
                    ModParticles.LIGHTNING_EXPLOSION.get(),
                    pos.x,
                    pos.y+0.1,
                    pos.z,

                    Math.cos(angle)*0.3,
                    0,
                    Math.sin(angle)*0.3
            );


        }



    }




    private static void createBolt(
            ClientLevel level,
            Vec3 start,
            Vec3 end
    ){


        int sections=25;


        for(int i=0;i<sections;i++){


            double progress =
                    i/(double)sections;


            Vec3 point =
                    start.lerp(end,progress);


            point =
                    point.add(
                            (level.random.nextDouble()-0.5),
                            (level.random.nextDouble()-0.5),
                            (level.random.nextDouble()-0.5)
                    );



            level.addParticle(
                    ModParticles.LIGHTNING_EXPLOSION.get(),
                    point.x,
                    point.y,
                    point.z,
                    0,
                    0,
                    0
            );



        }


    }


}